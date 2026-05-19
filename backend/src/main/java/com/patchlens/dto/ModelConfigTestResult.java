package com.patchlens.dto;

public record ModelConfigTestResult(
        boolean success,
        String message,
        long latencyMs
) {
}
