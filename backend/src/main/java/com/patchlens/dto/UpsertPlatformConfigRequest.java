package com.patchlens.dto;

import jakarta.validation.constraints.NotBlank;

public record UpsertPlatformConfigRequest(
        @NotBlank String provider,
        @NotBlank String apiBaseUrl,
        String accessToken,
        String webhookSecret,
        boolean enabled
) {
}
