package com.patchlens.dto;

import com.patchlens.domain.RepositoryConnection;

public record RepositoryDto(
        Long id,
        String provider,
        String owner,
        String name,
        String defaultBranch,
        boolean enabled
) {
    public static RepositoryDto from(RepositoryConnection repository) {
        return new RepositoryDto(
                repository.getId(),
                repository.getProvider().name(),
                repository.getOwner(),
                repository.getName(),
                repository.getDefaultBranch(),
                repository.isEnabled()
        );
    }
}
