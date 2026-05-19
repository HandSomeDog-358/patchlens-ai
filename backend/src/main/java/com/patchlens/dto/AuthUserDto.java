package com.patchlens.dto;

public record AuthUserDto(
        boolean authenticated,
        String username,
        String displayName,
        String role
) {
}
