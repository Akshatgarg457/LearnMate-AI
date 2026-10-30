package com.learnmate.backend.peer.dto;

import jakarta.validation.constraints.*;

public record CreateSessionRequest(
        @NotBlank(message = "Topic is required")
        @Size(max = 200)
        String topic,

        @Size(max = 2000)
        String description,

        @Min(value = 5, message = "Minimum session length is 5 minutes")
        @Max(value = 240, message = "Maximum session length is 240 minutes")
        int requestedMinutes
) {}
