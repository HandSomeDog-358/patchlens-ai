package com.patchlens.service;

import com.patchlens.domain.RepositoryConnection;
import com.patchlens.domain.RepositoryProvider;
import org.springframework.stereotype.Service;

@Service
public class PullRequestContextLoader {

    private final GiteePullRequestClient giteePullRequestClient;
    private final GiteaPullRequestClient giteaPullRequestClient;
    private final GithubPullRequestClient githubPullRequestClient;

    public PullRequestContextLoader(
            GiteePullRequestClient giteePullRequestClient,
            GiteaPullRequestClient giteaPullRequestClient,
            GithubPullRequestClient githubPullRequestClient
    ) {
        this.giteePullRequestClient = giteePullRequestClient;
        this.giteaPullRequestClient = giteaPullRequestClient;
        this.githubPullRequestClient = githubPullRequestClient;
    }

    public PullRequestContext load(RepositoryConnection repository, int prNumber, String fallbackCommitSha) {
        if (repository.getProvider() == RepositoryProvider.GITHUB && githubPullRequestClient.isConfigured()) {
            return githubPullRequestClient.fetch(repository, prNumber, fallbackCommitSha);
        }
        if (repository.getProvider() == RepositoryProvider.GITEE && giteePullRequestClient.isConfigured()) {
            return giteePullRequestClient.fetch(repository, prNumber, fallbackCommitSha);
        }
        if (repository.getProvider() == RepositoryProvider.GITEA && giteaPullRequestClient.isConfigured()) {
            return giteaPullRequestClient.fetch(repository, prNumber, fallbackCommitSha);
        }
        return PullRequestContext.empty(fallbackCommitSha);
    }

    public PullRequestContext loadCommit(RepositoryConnection repository, String commitSha) {
        if (repository.getProvider() == RepositoryProvider.GITHUB && githubPullRequestClient.isConfigured()) {
            return githubPullRequestClient.fetchCommit(repository, commitSha);
        }
        if (repository.getProvider() == RepositoryProvider.GITEE && giteePullRequestClient.isConfigured()) {
            return giteePullRequestClient.fetchCommit(repository, commitSha);
        }
        if (repository.getProvider() == RepositoryProvider.GITEA && giteaPullRequestClient.isConfigured()) {
            return giteaPullRequestClient.fetchCommit(repository, commitSha);
        }
        return PullRequestContext.empty(commitSha);
    }
}
