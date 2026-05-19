package com.patchlens.dto;

public record RepositoryBranchDto(
        String name,
        String latestCommitSha
) {
    public static RepositoryBranchDto of(String name, String latestCommitSha) {
        return new RepositoryBranchDto(name == null ? "" : name, latestCommitSha == null ? "" : latestCommitSha);
    }
}
