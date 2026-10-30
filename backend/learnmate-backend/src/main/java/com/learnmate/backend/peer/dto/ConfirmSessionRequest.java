package com.learnmate.backend.peer.dto;

import jakarta.validation.constraints.*;

public record ConfirmSessionRequest(
        @Min(1) @Max(5)
        Integer rating,   // learner only

        @Size(max = 1000)
        String feedback   // learner only
) {}
