package com.patchlens.dto;

public record RepositoryCommitDto(
        String sha,
        String shortSha,
        String message,
        String authorName,
        String authoredAt,
        String webUrl
) {
    public static RepositoryCommitDto of(
            String sha,
            String message,
            String authorName,
            String authoredAt,
            String webUrl
    ) {
        String normalizedSha = sha == null ? "" : sha;
        String shortSha = normalizedSha.length() > 12 ? normalizedSha.substring(0, 12) : normalizedSha;
        return new RepositoryCommitDto(normalizedSha, shortSha, message, authorName, authoredAt, webUrl);
    }
}
