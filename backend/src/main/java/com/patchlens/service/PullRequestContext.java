package com.patchlens.service;

import java.util.List;

public record PullRequestContext(
        String title,
        String description,
        String commitSha,
        String diff,
        List<String> changedFiles
) {
    public static PullRequestContext empty(String commitSha) {
        return new PullRequestContext("", "", commitSha, "", List.of());
    }
}
