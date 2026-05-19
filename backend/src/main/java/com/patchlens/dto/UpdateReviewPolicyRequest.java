package com.patchlens.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateReviewPolicyRequest(
        String language,
        @Min(0) @Max(1) double minConfidence,
        @Min(0) @Max(20) int maxInlineComments,
        boolean enableSummary,
        boolean enableInlineComments,
        boolean enableSuggestedPatch,
        String ignoredPaths,
        String focusPaths
) {
}
