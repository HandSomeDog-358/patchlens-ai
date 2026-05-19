package com.patchlens.service;

import com.patchlens.domain.FindingSeverity;
import java.util.List;

public interface AiReviewClient {

    ReviewResult review(ReviewInput input);

    record ReviewInput(
            String repositoryName,
            String targetType,
            String targetRef,
            int prNumber,
            String commitSha,
            String title,
            String description,
            String diff,
            java.util.List<String> changedFiles,
            String language,
            double minConfidence,
            int maxInlineComments,
            boolean enableSummary,
            boolean enableInlineComments,
            boolean enableSuggestedPatch,
            String ignoredPaths,
            String focusPaths
    ) {
    }

    record ReviewResult(
            String summary,
            List<FindingCandidate> findings
    ) {
    }

    record FindingCandidate(
            FindingSeverity severity,
            double confidence,
            String filePath,
            int lineNumber,
            String title,
            String description,
            String evidence,
            String suggestion,
            String patch
    ) {
    }
}
