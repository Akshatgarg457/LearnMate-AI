package com.learnmate.backend.exam.dto;

import com.learnmate.backend.exam.entity.CodeSubmission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitCodeRequest(
        @NotNull(message = "Language is required")
        CodeSubmission.Language language,

        @NotBlank(message = "Code is required")
        @Size(max = 65535, message = "Code too large")
        String code
) {}
