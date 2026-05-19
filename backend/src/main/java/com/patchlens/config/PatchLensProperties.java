package com.patchlens.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "patchlens")
public record PatchLensProperties(
        String aiReviewer,
        Review review,
        Github github,
        Gitee gitee,
        Gitea gitea
) {
    public record Review(int defaultMaxInlineComments, double defaultMinConfidence) {
    }

    public record Github(String webhookSecret, String apiBaseUrl, String accessToken) {
    }

    public record Gitee(String webhookSecret, String apiBaseUrl, String accessToken) {
    }

    public record Gitea(String webhookSecret, String apiBaseUrl, String accessToken) {
    }
}
