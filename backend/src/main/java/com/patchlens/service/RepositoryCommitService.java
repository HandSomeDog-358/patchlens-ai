package com.patchlens.service;

import com.patchlens.domain.RepositoryConnection;
import com.patchlens.domain.RepositoryProvider;
import com.patchlens.dto.RepositoryBranchDto;
import com.patchlens.dto.RepositoryCommitDto;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RepositoryCommitService {

    private final RepositoryService repositoryService;
    private final GithubPullRequestClient githubPullRequestClient;
    private final GiteePullRequestClient giteePullRequestClient;
    private final GiteaPullRequestClient giteaPullRequestClient;

    public RepositoryCommitService(
            RepositoryService repositoryService,
            GithubPullRequestClient githubPullRequestClient,
            GiteePullRequestClient giteePullRequestClient,
            GiteaPullRequestClient giteaPullRequestClient
    ) {
        this.repositoryService = repositoryService;
        this.githubPullRequestClient = githubPullRequestClient;
        this.giteePullRequestClient = giteePullRequestClient;
        this.giteaPullRequestClient = giteaPullRequestClient;
    }

    @Transactional(readOnly = true)
    public List<RepositoryCommitDto> listRecentCommits(Long repositoryId, String branch, String author, int limit) {
        RepositoryConnection repository = repositoryService.getRepository(repositoryId);
        int normalizedLimit = Math.max(1, Math.min(limit, 50));
        int fetchLimit = StringUtils.hasText(author) ? Math.max(normalizedLimit, 50) : normalizedLimit;
        String branchName = StringUtils.hasText(branch) ? branch.trim() : repository.getDefaultBranch();
        List<RepositoryCommitDto> commits;
        if (repository.getProvider() == RepositoryProvider.GITHUB && githubPullRequestClient.isConfigured()) {
            commits = githubPullRequestClient.listRecentCommits(repository, branchName, author, fetchLimit);
            return filterAndLimit(commits, author, normalizedLimit);
        }
        if (repository.getProvider() == RepositoryProvider.GITEE && giteePullRequestClient.isConfigured()) {
            commits = giteePullRequestClient.listRecentCommits(repository, branchName, author, fetchLimit);
            return filterAndLimit(commits, author, normalizedLimit);
        }
        if (repository.getProvider() == RepositoryProvider.GITEA && giteaPullRequestClient.isConfigured()) {
            commits = giteaPullRequestClient.listRecentCommits(repository, branchName, author, fetchLimit);
            return filterAndLimit(commits, author, normalizedLimit);
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public List<RepositoryBranchDto> listBranches(Long repositoryId, int limit) {
        RepositoryConnection repository = repositoryService.getRepository(repositoryId);
        int normalizedLimit = Math.max(1, Math.min(limit, 100));
        if (repository.getProvider() == RepositoryProvider.GITHUB && githubPullRequestClient.isConfigured()) {
            return githubPullRequestClient.listBranches(repository, normalizedLimit);
        }
        if (repository.getProvider() == RepositoryProvider.GITEE && giteePullRequestClient.isConfigured()) {
            return giteePullRequestClient.listBranches(repository, normalizedLimit);
        }
        if (repository.getProvider() == RepositoryProvider.GITEA && giteaPullRequestClient.isConfigured()) {
            return giteaPullRequestClient.listBranches(repository, normalizedLimit);
        }
        return List.of();
    }

    private List<RepositoryCommitDto> filterAndLimit(List<RepositoryCommitDto> commits, String author, int limit) {
        if (!StringUtils.hasText(author)) {
            return commits.stream().limit(limit).toList();
        }
        String keyword = author.trim().toLowerCase(Locale.ROOT);
        return commits.stream()
                .filter(commit -> commit.authorName() != null
                        && commit.authorName().toLowerCase(Locale.ROOT).contains(keyword))
                .limit(limit)
                .toList();
    }
}
