package com.patchlens.service;

import com.patchlens.domain.FindingSeverity;
import com.patchlens.domain.ReviewConclusion;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReviewPolicyApplier {

    public List<AiReviewClient.FindingCandidate> apply(
            List<AiReviewClient.FindingCandidate> candidates,
            ReviewPolicySnapshot policy
    ) {
        if (!policy.enableInlineComments()) {
            return List.of();
        }
        int limit = Math.max(0, Math.min(policy.maxInlineComments(), 20));
        if (limit == 0) {
            return List.of();
        }
        List<String> ignoredPatterns = lines(policy.ignoredPaths());
        return candidates.stream()
                .filter(candidate -> candidate.confidence() >= policy.minConfidence())
                .filter(candidate -> !isIgnored(candidate.filePath(), ignoredPatterns))
                .sorted(Comparator
                        .comparingInt((AiReviewClient.FindingCandidate candidate) -> severityRank(candidate.severity()))
                        .thenComparing(AiReviewClient.FindingCandidate::confidence, Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }

    public ReviewConclusion conclude(List<AiReviewClient.FindingCandidate> findings) {
        if (findings.stream().anyMatch(finding -> finding.severity() == FindingSeverity.CRITICAL
                || finding.severity() == FindingSeverity.HIGH)) {
            return ReviewConclusion.BLOCK;
        }
        if (!findings.isEmpty()) {
            return ReviewConclusion.WARN;
        }
        return ReviewConclusion.PASS;
    }

    private int severityRank(FindingSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    private boolean isIgnored(String filePath, List<String> patterns) {
        if (!StringUtils.hasText(filePath) || patterns.isEmpty()) {
            return false;
        }
        return patterns.stream().anyMatch(pattern -> matches(pattern, filePath));
    }

    private boolean matches(String pattern, String filePath) {
        try {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            return matcher.matches(java.nio.file.Path.of(filePath));
        } catch (RuntimeException ex) {
            return filePath.contains(pattern);
        }
    }

    private List<String> lines(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return value.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(line -> !line.startsWith("#"))
                .toList();
    }
}
