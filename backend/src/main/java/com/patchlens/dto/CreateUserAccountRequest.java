package com.patchlens.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserAccountRequest(
        @NotBlank String username,
        @NotBlank String displayName,
        @NotBlank String password,
        boolean enabled
) {
}
