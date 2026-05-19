package com.patchlens.dto;

import com.patchlens.domain.ReviewFeedback;
import com.patchlens.domain.ReviewFinding;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ReviewFindingDto(
        Long id,
        String severity,
        double confidence,
        String filePath,
        int lineNumber,
        String title,
        String description,
        String evidence,
        String suggestion,
        String patch,
        boolean published,
        Map<String, Long> feedbackCounts,
        String latestFeedbackValue,
        String latestFeedbackNote,
        Instant latestFeedbackAt
) {
    public static ReviewFindingDto from(ReviewFinding finding) {
        return from(finding, List.of());
    }

    public static ReviewFindingDto from(ReviewFinding finding, List<ReviewFeedback> feedbacks) {
        Map<String, Long> feedbackCounts = feedbacks.stream()
                .collect(Collectors.groupingBy(ReviewFeedback::getValue, Collectors.counting()));
        ReviewFeedback latestFeedback = feedbacks.isEmpty() ? null : feedbacks.get(0);
        return new ReviewFindingDto(
                finding.getId(),
                finding.getSeverity().name(),
                finding.getConfidence(),
                finding.getFilePath(),
                finding.getLineNumber(),
                finding.getTitle(),
                finding.getDescription(),
                finding.getEvidence(),
                finding.getSuggestion(),
                finding.getPatch(),
                finding.isPublished(),
                feedbackCounts,
                latestFeedback == null ? "" : latestFeedback.getValue(),
                latestFeedback == null ? "" : latestFeedback.getNote(),
                latestFeedback == null ? null : latestFeedback.getCreatedAt()
        );
    }
}
