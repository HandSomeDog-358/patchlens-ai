package com.patchlens.dto;

import jakarta.validation.constraints.NotBlank;

public record ListModelsRequest(
        @NotBlank String baseUrl,
        @NotBlank String apiKey
) {
}
