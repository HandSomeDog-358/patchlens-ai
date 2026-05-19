package com.patchlens.dto;

import com.patchlens.domain.ReviewPolicy;

public record ReviewPolicyDto(
        Long id,
        Long repositoryId,
        String language,
        double minConfidence,
        int maxInlineComments,
        boolean enableSummary,
        boolean enableInlineComments,
        boolean enableSuggestedPatch,
        String ignoredPaths,
        String focusPaths
) {
    public static ReviewPolicyDto from(ReviewPolicy policy) {
        return new ReviewPolicyDto(
                policy.getId(),
                policy.getRepository().getId(),
                policy.getLanguage(),
                policy.getMinConfidence(),
                policy.getMaxInlineComments(),
                policy.isEnableSummary(),
                policy.isEnableInlineComments(),
                policy.isEnableSuggestedPatch(),
                policy.getIgnoredPaths(),
                policy.getFocusPaths()
        );
    }
}
