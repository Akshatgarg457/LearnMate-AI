package com.learnmate.backend.exam.judge;

/**
 * Abstraction over the external code-execution judge.
 * Implementations MUST NOT execute code inside the Spring Boot process.
 */
public interface JudgeProvider {

    /**
     * Run one program synchronously against a single standard-input payload.
     *
     * @param language      JAVA, PYTHON, CPP or JAVASCRIPT
     * @param code          untrusted source code — never evaluated locally
     * @param stdin         test input
     * @param timeLimitMs   CPU limit
     * @param memoryLimitMb memory limit
     * @return the judge result (status, output, timing, memory)
     */
    JudgeResult execute(String language, String code, String stdin,
                        int timeLimitMs, int memoryLimitMb);
}
