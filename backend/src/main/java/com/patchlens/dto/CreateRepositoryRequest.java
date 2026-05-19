package com.patchlens.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRepositoryRequest(
        @NotBlank String provider,
        @NotBlank String owner,
        @NotBlank String name,
        String defaultBranch
) {
}
