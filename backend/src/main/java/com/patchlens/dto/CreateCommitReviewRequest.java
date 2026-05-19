package com.patchlens.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCommitReviewRequest(
        @NotBlank String commitSha
) {
}
