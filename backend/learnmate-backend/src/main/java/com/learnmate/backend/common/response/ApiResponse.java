package com.learnmate.backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standard envelope for all API responses.
 *
 * <p>Success: {@code {"success":true,"data":{...},"timestamp":"..."}}
 * <br>Error:   {@code {"success":false,"error":"message","timestamp":"..."}}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String error;
    private final String timestamp;

    private ApiResponse(boolean success, T data, String error) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.timestamp = Instant.now().toString();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }

    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public String getError() { return error; }
    public String getTimestamp() { return timestamp; }
}
