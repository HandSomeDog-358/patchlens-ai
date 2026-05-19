package com.patchlens.service;

import com.patchlens.domain.RepositoryConnection;
import com.patchlens.domain.RepositoryProvider;
import org.springframework.stereotype.Service;

@Service
public class PullRequestCommentService {

    private final GiteePullRequestClient giteePullRequestClient;
    private final GiteaPullRequestClient giteaPullRequestClient;
    private final GithubPullRequestClient githubPullRequestClient;

    public PullRequestCommentService(
            GiteePullRequestClient giteePullRequestClient,
            GiteaPullRequestClient giteaPullRequestClient,
            GithubPullRequestClient githubPullRequestClient
    ) {
        this.giteePullRequestClient = giteePullRequestClient;
        this.giteaPullRequestClient = giteaPullRequestClient;
        this.githubPullRequestClient = githubPullRequestClient;
    }

    public String publishSummary(RepositoryConnection repository, int prNumber, String body) {
        if (repository.getProvider() == RepositoryProvider.GITHUB) {
            return githubPullRequestClient.createPullRequestComment(repository, prNumber, body);
        }
        if (repository.getProvider() == RepositoryProvider.GITEE) {
            return giteePullRequestClient.createPullRequestComment(repository, prNumber, body);
        }
        if (repository.getProvider() == RepositoryProvider.GITEA) {
            return giteaPullRequestClient.createPullRequestComment(repository, prNumber, body);
        }
        throw new IllegalArgumentException("当前平台暂不支持 PR 评论回写：" + repository.getProvider());
    }
}
