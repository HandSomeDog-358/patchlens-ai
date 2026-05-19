package com.patchlens.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReviewFeedbackRequest(
        @NotBlank String value,
        @Size(max = 2000) String note
) {
}
