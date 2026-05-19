package com.patchlens.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserAccountRequest(
        @NotBlank String displayName,
        String password,
        boolean enabled
) {
}
