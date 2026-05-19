package com.patchlens.dto;

import com.patchlens.domain.ReviewTask;
import java.time.Instant;

public record ReviewTaskDto(
        Long id,
        Long repositoryId,
        String repositoryName,
        String targetType,
        int prNumber,
        String commitSha,
        String status,
        String conclusion,
        String triggerType,
        String summary,
        String errorMessage,
        String publishError,
        Instant startedAt,
        Instant finishedAt,
        Instant publishedAt,
        Instant createdAt
) {
    public static ReviewTaskDto from(ReviewTask task) {
        return new ReviewTaskDto(
                task.getId(),
                task.getRepository().getId(),
                task.getRepository().getOwner() + "/" + task.getRepository().getName(),
                task.getTargetType().name(),
                task.getPrNumber(),
                task.getCommitSha(),
                task.getStatus().name(),
                task.getConclusion() == null ? "" : task.getConclusion().name(),
                task.getTriggerType().name(),
                task.getSummary(),
                task.getErrorMessage(),
                task.getPublishError(),
                task.getStartedAt(),
                task.getFinishedAt(),
                task.getPublishedAt(),
                task.getCreatedAt()
        );
    }
}
