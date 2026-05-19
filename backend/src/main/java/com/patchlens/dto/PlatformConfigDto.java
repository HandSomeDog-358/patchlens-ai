package com.patchlens.dto;

import com.patchlens.domain.PlatformConfig;

public record PlatformConfigDto(
        Long id,
        String provider,
        String apiBaseUrl,
        boolean hasAccessToken,
        boolean hasWebhookSecret,
        boolean enabled
) {
    public static PlatformConfigDto from(PlatformConfig config) {
        return new PlatformConfigDto(
                config.getId(),
                config.getProvider().name(),
                config.getApiBaseUrl(),
                config.getAccessTokenEncrypted() != null && !config.getAccessTokenEncrypted().isBlank(),
                config.getWebhookSecretEncrypted() != null && !config.getWebhookSecretEncrypted().isBlank(),
                config.isEnabled()
        );
    }
}
