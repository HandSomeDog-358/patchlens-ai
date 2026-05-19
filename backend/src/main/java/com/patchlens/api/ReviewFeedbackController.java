package com.patchlens.api;

import com.patchlens.dto.CreateReviewFeedbackRequest;
import com.patchlens.dto.ReviewFeedbackDto;
import com.patchlens.service.AuditLogService;
import com.patchlens.service.ReviewFeedbackService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/findings")
public class ReviewFeedbackController {

    private final ReviewFeedbackService reviewFeedbackService;
    private final AuditLogService auditLogService;

    public ReviewFeedbackController(ReviewFeedbackService reviewFeedbackService, AuditLogService auditLogService) {
        this.reviewFeedbackService = reviewFeedbackService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/{findingId}/feedback")
    public ReviewFeedbackDto create(
            @PathVariable Long findingId,
            @Valid @RequestBody CreateReviewFeedbackRequest request
    ) {
        ReviewFeedbackDto dto = reviewFeedbackService.create(findingId, request);
        auditLogService.record("REVIEW_FEEDBACK_CREATE", "REVIEW_FINDING", findingId, dto.value());
        return dto;
    }
}
