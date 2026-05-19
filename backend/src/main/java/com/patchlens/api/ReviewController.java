package com.patchlens.api;

import com.patchlens.dto.PageResponse;
import com.patchlens.dto.ReviewGovernanceDto;
import com.patchlens.dto.ReviewFindingDto;
import com.patchlens.dto.ReviewTaskDto;
import com.patchlens.service.AuditLogService;
import com.patchlens.service.ReviewService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final AuditLogService auditLogService;

    public ReviewController(ReviewService reviewService, AuditLogService auditLogService) {
        this.reviewService = reviewService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public PageResponse<ReviewTaskDto> listReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        return reviewService.listReviews(page, size, status);
    }

    @GetMapping("/governance")
    public ReviewGovernanceDto governance(@RequestParam(defaultValue = "60") int stuckMinutes) {
        return reviewService.governance(stuckMinutes);
    }

    @GetMapping("/{reviewId}")
    public ReviewTaskDto getReview(@PathVariable Long reviewId) {
        return reviewService.getReview(reviewId);
    }

    @PostMapping("/{reviewId}/rerun")
    public ReviewTaskDto rerun(@PathVariable Long reviewId) {
        ReviewTaskDto dto = reviewService.rerun(reviewId);
        auditLogService.record("REVIEW_RERUN", "REVIEW", reviewId, dto.repositoryName());
        return dto;
    }

    @PostMapping("/{reviewId}/cancel")
    public ReviewTaskDto cancel(@PathVariable Long reviewId) {
        ReviewTaskDto dto = reviewService.cancel(reviewId);
        auditLogService.record("REVIEW_CANCEL", "REVIEW", reviewId, dto.repositoryName());
        return dto;
    }

    @PostMapping("/cleanup-stuck")
    public List<ReviewTaskDto> cleanupStuck(@RequestParam(defaultValue = "60") int minutes) {
        List<ReviewTaskDto> result = reviewService.cleanupStuck(minutes);
        auditLogService.record("REVIEW_CLEANUP_STUCK", "REVIEW", "", "minutes=" + minutes + ", count=" + result.size());
        return result;
    }

    @GetMapping("/{reviewId}/findings")
    public List<ReviewFindingDto> listFindings(@PathVariable Long reviewId) {
        return reviewService.listFindings(reviewId);
    }
}
