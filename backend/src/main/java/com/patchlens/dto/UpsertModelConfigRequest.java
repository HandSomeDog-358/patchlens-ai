package com.patchlens.dto;

import jakarta.validation.constraints.NotBlank;

public record UpsertModelConfigRequest(
        @NotBlank String provider,
        @NotBlank String baseUrl,
        @NotBlank String modelName,
        String apiKey,
        boolean enabled
) {
}
