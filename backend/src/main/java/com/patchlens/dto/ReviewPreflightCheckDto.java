package com.patchlens.dto;

public record ReviewPreflightCheckDto(
        String name,
        String status,
        String message
) {
}
