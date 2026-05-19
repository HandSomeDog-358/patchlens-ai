package com.patchlens.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateReviewRequest(
        @Min(1) int prNumber,
        @NotBlank String commitSha
) {
}
