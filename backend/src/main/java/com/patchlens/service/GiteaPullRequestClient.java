package com.patchlens.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.patchlens.domain.RepositoryConnection;
import com.patchlens.domain.RepositoryProvider;
import com.patchlens.dto.RepositoryBranchDto;
import com.patchlens.dto.RepositoryCommitDto;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GiteaPullRequestClient {

    private final PlatformConfigService platformConfigService;
    private final RestClient restClient;

    public GiteaPullRequestClient(PlatformConfigService platformConfigService, RestClient.Builder restClientBuilder) {
        this.platformConfigService = platformConfigService;
        this.restClient = restClientBuilder.build();
    }

    public boolean isConfigured() {
        return platformConfigService.resolve(RepositoryProvider.GITEA)
                .map(PlatformConfigService.ResolvedPlatformConfig::hasApiAccess)
                .orElse(false);
    }

    public PullRequestContext fetch(RepositoryConnection repository, int prNumber, String fallbackCommitSha) {
        PlatformConfigService.ResolvedPlatformConfig config = platformConfigService.resolve(RepositoryProvider.GITEA)
                .filter(PlatformConfigService.ResolvedPlatformConfig::hasApiAccess)
                .orElseThrow(() -> new IllegalStateException("Gitea platform config is not ready"));

        JsonNode pr = restClient.get()
                .uri(buildPullRequestUri(config, repository, prNumber))
                .header(HttpHeaders.AUTHORIZATION, "token " + config.accessToken())
                .retrieve()
                .body(JsonNode.class);

        JsonNode files = restClient.get()
                .uri(buildPullRequestFilesUri(config, repository, prNumber))
                .header(HttpHeaders.AUTHORIZATION, "token " + config.accessToken())
                .retrieve()
                .body(JsonNode.class);

        String diff = restClient.get()
                .uri(buildPullRequestDiffUri(config, repository, prNumber))
                .header(HttpHeaders.AUTHORIZATION, "token " + config.accessToken())
                .retrieve()
                .body(String.class);

        String title = text(pr, "title");
        String description = firstText(pr == null ? null : pr.get("body"), pr == null ? null : pr.get("description"), "");
        String commitSha = firstText(pr == null ? null : pr.at("/head/sha"), pr == null ? null : pr.at("/head/ref"), fallbackCommitSha);
        List<String> changedFiles = extractChangedFiles(files);

        return new PullRequestContext(title, description, commitSha, diff == null ? "" : diff, changedFiles);
    }

    public PullRequestContext fetchCommit(RepositoryConnection repository, String commitSha) {
        PlatformConfigService.ResolvedPlatformConfig config = platformConfigService.resolve(RepositoryProvider.GITEA)
                .filter(PlatformConfigService.ResolvedPlatformConfig::hasApiAccess)
                .orElseThrow(() -> new IllegalStateException("Gitea platform config is not ready"));

        JsonNode commit = restClient.get()
                .uri(buildCommitUri(config, repository, commitSha))
                .header(HttpHeaders.AUTHORIZATION, "token " + config.accessToken())
                .retrieve()
                .body(JsonNode.class);

        String diff = restClient.get()
                .uri(buildCommitDiffUri(config, repository, commitSha))
                .header(HttpHeaders.AUTHORIZATION, "token " + config.accessToken())
                .retrieve()
                .body(String.class);

        String title = firstLine(firstText(
                commit == null ? null : commit.at("/commit/message"),
                commit == null ? null : commit.get("message"),
                "Commit " + commitSha
        ));
        String description = firstText(
                commit == null ? null : commit.at("/commit/author/name"),
                commit == null ? null : commit.at("/author/login"),
                ""
        );

        return new PullRequestContext(title, description, commitSha, diff == null ? "" : diff, List.of());
    }

    public List<RepositoryCommitDto> listRecentCommits(RepositoryConnection repository, String branch, String author, int limit) {
        PlatformConfigService.ResolvedPlatformConfig config = platformConfigService.resolve(RepositoryProvider.GITEA)
                .filter(PlatformConfigService.ResolvedPlatformConfig::hasApiAccess)
                .orElseThrow(() -> new IllegalStateException("Gitea platform config is not ready"));

        JsonNode commits = restClient.get()
                .uri(buildCommitsUri(config, repository, branch, author, limit))
                .header(HttpHeaders.AUTHORIZATION, "token " + config.accessToken())
                .retrieve()
                .body(JsonNode.class);

        List<RepositoryCommitDto> result = new ArrayList<>();
        if (commits == null || !commits.isArray()) {
            return result;
        }
        for (JsonNode commit : commits) {
            String sha = firstText(commit.get("sha"), commit.get("id"), "");
            String message = firstLine(firstText(commit.at("/commit/message"), commit.get("message"), "无提交信息"));
            String authorName = firstText(commit.at("/commit/author/name"), commit.at("/author/login"), "");
            String authoredAt = firstText(commit.at("/commit/author/date"), commit.at("/author/created"), "");
            String webUrl = firstText(commit.get("html_url"), commit.get("url"), "");
            if (StringUtils.hasText(sha)) {
                result.add(RepositoryCommitDto.of(sha, message, authorName, authoredAt, webUrl));
            }
        }
        return result;
    }

    public List<RepositoryBranchDto> listBranches(RepositoryConnection repository, int limit) {
        PlatformConfigService.ResolvedPlatformConfig config = platformConfigService.resolve(RepositoryProvider.GITEA)
                .filter(PlatformConfigService.ResolvedPlatformConfig::hasApiAccess)
                .orElseThrow(() -> new IllegalStateException("Gitea platform config is not ready"));

        JsonNode branches = restClient.get()
                .uri(buildBranchesUri(config, repository, limit))
                .header(HttpHeaders.AUTHORIZATION, "token " + config.accessToken())
                .retrieve()
                .body(JsonNode.class);

        List<RepositoryBranchDto> result = new ArrayList<>();
        if (branches == null || !branches.isArray()) {
            return result;
        }
        for (JsonNode branch : branches) {
            String name = firstText(branch.get("name"), branch.get("branch_name"), "");
            String sha = firstText(branch.at("/commit/id"), branch.at("/commit/sha"), "");
            if (StringUtils.hasText(name)) {
                result.add(RepositoryBranchDto.of(name, sha));
            }
        }
        return result;
    }

    public String createPullRequestComment(RepositoryConnection repository, int prNumber, String body) {
        PlatformConfigService.ResolvedPlatformConfig config = platformConfigService.resolve(RepositoryProvider.GITEA)
                .filter(PlatformConfigService.ResolvedPlatformConfig::hasApiAccess)
                .orElseThrow(() -> new IllegalStateException("Gitea platform config is not ready"));
        JsonNode response = restClient.post()
                .uri(buildIssueCommentsUri(config, repository, prNumber))
                .header(HttpHeaders.AUTHORIZATION, "token " + config.accessToken())
                .body(Map.of("body", body))
                .retrieve()
                .body(JsonNode.class);
        return firstText(response == null ? null : response.get("id"), response == null ? null : response.get("html_url"), "");
    }

    private URI buildPullRequestUri(
            PlatformConfigService.ResolvedPlatformConfig config,
            RepositoryConnection repository,
            int prNumber
    ) {
        return baseBuilder(config)
                .pathSegment("repos", repository.getOwner(), repository.getName(), "pulls", String.valueOf(prNumber))
                .build()
                .toUri();
    }

    private URI buildPullRequestFilesUri(
            PlatformConfigService.ResolvedPlatformConfig config,
            RepositoryConnection repository,
            int prNumber
    ) {
        return baseBuilder(config)
                .pathSegment("repos", repository.getOwner(), repository.getName(), "pulls", String.valueOf(prNumber), "files")
                .build()
                .toUri();
    }

    private URI buildPullRequestDiffUri(
            PlatformConfigService.ResolvedPlatformConfig config,
            RepositoryConnection repository,
            int prNumber
    ) {
        return baseBuilder(config)
                .pathSegment("repos", repository.getOwner(), repository.getName(), "pulls", String.valueOf(prNumber) + ".diff")
                .build()
                .toUri();
    }

    private URI buildIssueCommentsUri(
            PlatformConfigService.ResolvedPlatformConfig config,
            RepositoryConnection repository,
            int prNumber
    ) {
        return baseBuilder(config)
                .pathSegment("repos", repository.getOwner(), repository.getName(), "issues", String.valueOf(prNumber), "comments")
                .build()
                .toUri();
    }

    private URI buildCommitUri(
            PlatformConfigService.ResolvedPlatformConfig config,
            RepositoryConnection repository,
            String commitSha
    ) {
        return baseBuilder(config)
                .pathSegment("repos", repository.getOwner(), repository.getName(), "git", "commits", commitSha)
                .build()
                .toUri();
    }

    private URI buildCommitDiffUri(
            PlatformConfigService.ResolvedPlatformConfig config,
            RepositoryConnection repository,
            String commitSha
    ) {
        return baseBuilder(config)
                .pathSegment("repos", repository.getOwner(), repository.getName(), "git", "commits", commitSha + ".diff")
                .build()
                .toUri();
    }

    private URI buildCommitsUri(
            PlatformConfigService.ResolvedPlatformConfig config,
            RepositoryConnection repository,
            String branch,
            String author,
            int limit
    ) {
        UriComponentsBuilder builder = baseBuilder(config)
                .pathSegment("repos", repository.getOwner(), repository.getName(), "commits")
                .queryParam("sha", StringUtils.hasText(branch) ? branch : repository.getDefaultBranch())
                .queryParam("page", 1)
                .queryParam("limit", limit);
        if (StringUtils.hasText(author)) {
            builder.queryParam("author", author.trim());
        }
        return builder.build().toUri();
    }

    private URI buildBranchesUri(
            PlatformConfigService.ResolvedPlatformConfig config,
            RepositoryConnection repository,
            int limit
    ) {
        return baseBuilder(config)
                .pathSegment("repos", repository.getOwner(), repository.getName(), "branches")
                .queryParam("page", 1)
                .queryParam("limit", limit)
                .build()
                .toUri();
    }

    private UriComponentsBuilder baseBuilder(PlatformConfigService.ResolvedPlatformConfig config) {
        return UriComponentsBuilder.fromUriString(config.apiBaseUrl());
    }

    private List<String> extractChangedFiles(JsonNode files) {
        List<String> result = new ArrayList<>();
        if (files == null || !files.isArray()) {
            return result;
        }
        for (JsonNode file : files) {
            String filename = firstText(file.get("filename"), file.get("name"), file.get("path"), "");
            if (StringUtils.hasText(filename)) {
                result.add(filename);
            }
        }
        return result;
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.get(fieldName) == null || node.get(fieldName).isNull()) {
            return "";
        }
        return node.get(fieldName).asText("");
    }

    private String firstText(JsonNode first, JsonNode second, String fallback) {
        return firstText(first, second, null, fallback);
    }

    private String firstText(JsonNode first, JsonNode second, JsonNode third, String fallback) {
        String firstValue = textValue(first);
        if (StringUtils.hasText(firstValue)) {
            return firstValue;
        }
        String secondValue = textValue(second);
        if (StringUtils.hasText(secondValue)) {
            return secondValue;
        }
        String thirdValue = textValue(third);
        if (StringUtils.hasText(thirdValue)) {
            return thirdValue;
        }
        return fallback;
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.asText("");
    }

    private String firstLine(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.lines().findFirst().orElse(value);
    }
}
