package com.patchlens.dto;

import java.util.List;

public record PlatformCapabilityDto(
        String provider,
        String displayName,
        boolean configurable,
        boolean configured,
        boolean enabled,
        String apiBaseUrl,
        boolean hasAccessToken,
        boolean hasWebhookSecret,
        long repositoryCount,
        long enabledRepositoryCount,
        boolean supportsRecentCommits,
        boolean supportsCommitReview,
        boolean supportsPullRequestReview,
        boolean supportsWebhook,
        boolean supportsInlineComments,
        boolean supportsSuggestedPatch,
        String status,
        List<String> gaps
) {
}
