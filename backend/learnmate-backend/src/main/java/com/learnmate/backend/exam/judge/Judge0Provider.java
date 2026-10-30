package com.learnmate.backend.exam.judge;

import com.learnmate.backend.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Judge0 adapter. Active only when JUDGE_BASE_URL points at a Judge0-compatible
 * service. Code is sent to that service; it is never compiled or run locally.
 */
@Component
@ConditionalOnExpression("!'${app.judge.base-url:NOT_CONFIGURED}'.equals('NOT_CONFIGURED')")
public class Judge0Provider implements JudgeProvider {

    private static final Map<String, Integer> LANGUAGE_IDS = Map.of(
            "JAVA", 62, "PYTHON", 71, "CPP", 54, "JAVASCRIPT", 63);

    private static final int MAX_POLL_ATTEMPTS = 60;
    private static final long POLL_INTERVAL_MS = 500;

    private final String baseUrl;
    private final String apiKey;
    private final RestTemplate restTemplate;

    public Judge0Provider(
            @Value("${app.judge.base-url}") String baseUrl,
            @Value("${app.judge.api-key}") String apiKey) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.apiKey = apiKey;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JudgeResult execute(String language, String code, String stdin,
                               int timeLimitMs, int memoryLimitMb) {
        Integer langId = LANGUAGE_IDS.get(language.toUpperCase());
        if (langId == null) throw ApiException.badRequest("Unsupported language: " + language);

        Map<String, Object> body = new HashMap<>();
        body.put("source_code", encode(code));
        body.put("language_id", langId);
        body.put("stdin", encode(stdin == null ? "" : stdin));
        body.put("cpu_time_limit", Math.max(1.0, timeLimitMs / 1000.0));
        body.put("wall_time_limit", Math.max(2.0, timeLimitMs / 1000.0 + 5));
        body.put("memory_limit", Math.max(16, memoryLimitMb) * 1024);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/submissions?base64_encoded=true&wait=true",
                    HttpMethod.POST, new HttpEntity<>(body, headers()), Map.class);
            Map<String, Object> result = response.getBody();
            if (result == null) throw ApiException.serviceUnavailable("The code judge returned an empty response");

            String token = (String) result.get("token");
            int attempts = 0;
            while (!finished(result) && token != null && attempts++ < MAX_POLL_ATTEMPTS) {
                Thread.sleep(POLL_INTERVAL_MS);
                result = fetch(token);
            }
            return toResult(result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ApiException.serviceUnavailable("Waiting for the code judge was interrupted");
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            if (status == 429) {
                throw ApiException.serviceUnavailable("The code judge is busy. Try again in a moment.");
            }
            throw ApiException.serviceUnavailable("The code judge rejected the request (HTTP " + status + ")");
        } catch (RestClientException e) {
            throw ApiException.serviceUnavailable("The code judge is unreachable. Try again in a moment.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetch(String token) {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/submissions/" + token + "?base64_encoded=true",
                HttpMethod.GET, new HttpEntity<>(headers()), Map.class);
        if (response.getBody() == null) throw ApiException.serviceUnavailable("The code judge returned an empty response");
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private boolean finished(Map<String, Object> body) {
        Map<String, Object> status = (Map<String, Object>) body.get("status");
        return status != null && ((Number) status.getOrDefault("id", 0)).intValue() >= 3;
    }

    @SuppressWarnings("unchecked")
    private JudgeResult toResult(Map<String, Object> body) {
        Map<String, Object> status = (Map<String, Object>) body.get("status");
        String description = status != null ? (String) status.get("description") : "Unknown";
        int statusId = status != null ? ((Number) status.getOrDefault("id", 0)).intValue() : 0;
        if (statusId == 1 || statusId == 2) {
            throw ApiException.serviceUnavailable("The code judge is still processing the submission");
        }
        return new JudgeResult(description,
                decode((String) body.get("stdout")),
                decode((String) body.get("stderr")),
                decode((String) body.get("compile_output")),
                toMs(body.get("time")),
                body.get("memory") instanceof Number n ? n.intValue() : null,
                true);
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank() && !apiKey.equals("NOT_CONFIGURED"))
            h.set("X-Auth-Token", apiKey);
        return h;
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        if (value == null) return null;
        try {
            return new String(Base64.getDecoder().decode(value), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private Integer toMs(Object time) {
        if (time == null) return null;
        try {
            return (int) (Double.parseDouble(time.toString()) * 1000);
        } catch (Exception e) {
            return null;
        }
    }
}
