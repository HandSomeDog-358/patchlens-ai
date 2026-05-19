package com.patchlens.service;

import com.patchlens.domain.ReviewFeedback;
import com.patchlens.domain.ReviewFinding;
import com.patchlens.dto.CreateReviewFeedbackRequest;
import com.patchlens.dto.ReviewFeedbackDto;
import com.patchlens.repository.ReviewFeedbackRepository;
import com.patchlens.repository.ReviewFindingRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReviewFeedbackService {

    private static final Set<String> ALLOWED_VALUES = Set.of("USEFUL", "FALSE_POSITIVE", "FIXED", "IGNORED");

    private final ReviewFindingRepository reviewFindingRepository;
    private final ReviewFeedbackRepository reviewFeedbackRepository;

    public ReviewFeedbackService(
            ReviewFindingRepository reviewFindingRepository,
            ReviewFeedbackRepository reviewFeedbackRepository
    ) {
        this.reviewFindingRepository = reviewFindingRepository;
        this.reviewFeedbackRepository = reviewFeedbackRepository;
    }

    @Transactional
    public ReviewFeedbackDto create(Long findingId, CreateReviewFeedbackRequest request) {
        ReviewFinding finding = reviewFindingRepository.findById(findingId)
                .orElseThrow(() -> new EntityNotFoundException("Review finding not found"));
        String value = normalizeValue(request.value());
        ReviewFeedback feedback = new ReviewFeedback();
        feedback.setFinding(finding);
        feedback.setValue(value);
        feedback.setNote(StringUtils.hasText(request.note()) ? request.note().trim() : null);
        return ReviewFeedbackDto.from(reviewFeedbackRepository.save(feedback));
    }

    private String normalizeValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("反馈类型不能为空");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_VALUES.contains(normalized)) {
            throw new IllegalArgumentException("不支持的反馈类型：" + value);
        }
        return normalized;
    }
}
