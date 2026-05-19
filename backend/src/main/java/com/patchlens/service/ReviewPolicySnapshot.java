package com.patchlens.service;

import com.patchlens.domain.ReviewPolicy;

public record ReviewPolicySnapshot(
        String language,
        double minConfidence,
        int maxInlineComments,
        boolean enableSummary,
        boolean enableInlineComments,
        boolean enableSuggestedPatch,
        String ignoredPaths,
        String focusPaths
) {
    public static ReviewPolicySnapshot from(ReviewPolicy policy) {
        return new ReviewPolicySnapshot(
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

    public static ReviewPolicySnapshot defaults() {
        return new ReviewPolicySnapshot("zh-CN", 0.75, 5, true, true, true, "", "");
    }
}
