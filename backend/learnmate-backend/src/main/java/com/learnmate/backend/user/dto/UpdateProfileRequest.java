package com.learnmate.backend.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 100, message = "Display name must be 2–100 characters")
        String displayName,

        @Size(max = 512, message = "Bio must not exceed 512 characters")
        String bio,

        @Size(max = 50, message = "Timezone must not exceed 50 characters")
        String timezone
) {}
