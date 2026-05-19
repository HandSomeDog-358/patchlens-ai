package com.patchlens.dto;

import com.patchlens.domain.ReviewFeedback;
import java.time.Instant;

public record ReviewFeedbackDto(
        Long id,
        Long findingId,
        String value,
        String note,
        Instant createdAt
) {
    public static ReviewFeedbackDto from(ReviewFeedback feedback) {
        return new ReviewFeedbackDto(
                feedback.getId(),
                feedback.getFinding().getId(),
                feedback.getValue(),
                feedback.getNote(),
                feedback.getCreatedAt()
        );
    }
}
