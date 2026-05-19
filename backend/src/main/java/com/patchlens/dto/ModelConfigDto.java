package com.patchlens.dto;

import com.patchlens.domain.ModelConfig;

public record ModelConfigDto(
        Long id,
        String provider,
        String baseUrl,
        String modelName,
        boolean hasApiKey,
        boolean enabled
) {
    public static ModelConfigDto from(ModelConfig config) {
        return new ModelConfigDto(
                config.getId(),
                config.getProvider(),
                config.getBaseUrl(),
                config.getModelName(),
                config.getApiKeyEncrypted() != null && !config.getApiKeyEncrypted().isBlank(),
                config.isEnabled()
        );
    }
}
