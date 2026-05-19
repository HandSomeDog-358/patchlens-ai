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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GithubPullRequestClient {

    private final PlatformConfigService platformConfigService;
    private final RestClient restClient;

    public GithubPullRequestClient(PlatformConfigService platformConfigService, RestClient.Builder restClientBuilder) {
        this.platformConfigService = platformConfigService;
        this.restClient = restClientBuilder.build();
    }

    public boolean isConfigured() {
        return platformConfigService.resolve(RepositoryProvider.GITHUB)
                .map(PlatformConfigService.ResolvedPlatformConfig::hasApiAccess)
                .orElse(false);
    }

    public PullRequestContext fetch(RepositoryConnection repository, int prNumber, String fallbackCommitSha) {
        PlatformConfigService.ResolvedPlatformConfig config = runtimeConfig();
        JsonNode pr = restClient.get()
                .uri(buildPullRequestUri(config, repository, prNumber))
                .headers(headers -> applyHeaders(headers, config))
                .retrieve()
                .body(JsonNode.class);
        JsonNode files = restClient.get()
                .uri(buildPullRequestFilesUri(config, repository, prNumber))
                .headers(headers -> applyHeaders(headers, config))
                .retrieve()
                .body(JsonNode.class);
        String title = text(pr, "title");
        String description = text(pr, "body");
        String commitSha = firstText(pr == null ? null : pr.at("/head/sha"), null, fallbackCommitSha);
        return new PullRequestContext(title, description, commitSha, buildDiff(files), extractChangedFiles(files));
    }

    public PullRequestContext fetchCommit(RepositoryConnection repository, String commitSha) {
        PlatformConfigService.ResolvedPlatformConfig config = runtimeConfig();
        JsonNode commit = restClient.get()
                .uri(buildCommitUri(config, repository, commitSha))
                .headers(headers -> applyHeaders(headers, config))
                .retrieve()
                .body(JsonNode.class);
        JsonNode files = commit == null ? null : commit.get("files");
        String title = firstLine(firstText(commit == null ? null : commit.at("/commit/message"), null, "Commit " + commitSha));
        String description = firstText(commit == null ? null : commit.at("/commit/author/name"), null, "");
        return new PullRequestContext(title, description, commitSha, buildDiff(files), extractChangedFiles(files));
    }

    public List<RepositoryCommitDto> listRecentCommits(RepositoryConnection repository, String branch, String author, int limit) {
        PlatformConfigService.ResolvedPlatformConfig config = runtimeConfig();
        JsonNode commits = restClient.get()
                .uri(buildCommitsUri(config, repository, branch, author, limit))
                .headers(headers -> applyHeaders(headers, config))
                .retrieve()
                .body(JsonNode.class);
        List<RepositoryCommitDto> result = new ArrayList<>();
        if (commits == null || !commits.isArray()) {
            return result;
        }
        for (JsonNode commit : commits) {
            String sha = firstText(commit.get("sha"), null, "");
            String message = firstLine(firstText(commit.at("/commit/message"), null, "无提交信息"));
            String authorName = firstText(commit.at("/commit/author/name"), commit.at("/author/login"), "");
            String authoredAt = firstText(commit.at("/commit/author/date"), null, "");
            String webUrl = firstText(commit.get("html_url"), null, "");
            if (StringUtils.hasText(sha)) {
                result.add(RepositoryCommitDto.of(sha, message, authorName, authoredAt, webUrl));
            }
        }
        return result;
    }

    public List<RepositoryBranchDto> listBranches(RepositoryConnection repository, int limit) {
        PlatformConfigService.ResolvedPlatformConfig config = runtimeConfig();
        JsonNode branches = restClient.get()
                .uri(buildBranchesUri(config, repository, limit))
                .headers(headers -> applyHeaders(headers, config))
                .retrieve()
                .body(JsonNode.class);
        List<RepositoryBranchDto> result = new ArrayList<>();
        if (branches == null || !branches.isArray()) {
            return result;
        }
        for (JsonNode branch : branches) {
            String name = firstText(branch.get("name"), null, "");
            String sha = firstText(branch.at("/commit/sha"), null, "");
            if (StringUtils.hasText(name)) {
                result.add(RepositoryBranchDto.of(name, sha));
            }
        }
        return result;
    }

    public String createPullRequestComment(RepositoryConnection repository, int prNumber, String body) {
        PlatformConfigService.ResolvedPlatformConfig config = runtimeConfig();
        JsonNode response = restClient.post()
                .uri(buildIssueCommentsUri(config, repository, prNumber))
                .headers(headers -> applyHeaders(headers, config))
                .body(Map.of("body", body))
                .retrieve()
                .body(JsonNode.class);
        return firstText(response == null ? null : response.get("id"), response == null ? null : response.get("html_url"), "");
    }

    private PlatformConfigService.ResolvedPlatformConfig runtimeConfig() {
        return platformConfigService.resolve(RepositoryProvider.GITHUB)
                .filter(PlatformConfigService.ResolvedPlatformConfig::hasApiAccess)
                .orElseThrow(() -> new IllegalStateException("GitHub platform config is not ready"));
    }

    private void applyHeaders(HttpHeaders headers, PlatformConfigService.ResolvedPlatformConfig config) {
        headers.setBearerAuth(config.accessToken());
        headers.setAccept(List.of(MediaType.valueOf("application/vnd.github+json")));
        headers.set("X-GitHub-Api-Version", "2022-11-28");
    }

    private URI buildPullRequestUri(PlatformConfigService.ResolvedPlatformConfig config, RepositoryConnection repository, int prNumber) {
        return baseBuilder(config).pathSegment("repos", repository.getOwner(), repository.getName(), "pulls", String.valueOf(prNumber)).build().toUri();
    }

    private URI buildPullRequestFilesUri(PlatformConfigService.ResolvedPlatformConfig config, RepositoryConnection repository, int prNumber) {
        return baseBuilder(config).pathSegment("repos", repository.getOwner(), repository.getName(), "pulls", String.valueOf(prNumber), "files").build().toUri();
    }

    private URI buildCommitUri(PlatformConfigService.ResolvedPlatformConfig config, RepositoryConnection repository, String commitSha) {
        return baseBuilder(config).pathSegment("repos", repository.getOwner(), repository.getName(), "commits", commitSha).build().toUri();
    }

    private URI buildCommitsUri(PlatformConfigService.ResolvedPlatformConfig config, RepositoryConnection repository, String branch, String author, int limit) {
        UriComponentsBuilder builder = baseBuilder(config)
                .pathSegment("repos", repository.getOwner(), repository.getName(), "commits")
                .queryParam("sha", StringUtils.hasText(branch) ? branch : repository.getDefaultBranch())
                .queryParam("per_page", limit);
        if (StringUtils.hasText(author)) {
            builder.queryParam("author", author.trim());
        }
        return builder.build().toUri();
    }

    private URI buildBranchesUri(PlatformConfigService.ResolvedPlatformConfig config, RepositoryConnection repository, int limit) {
        return baseBuilder(config)
                .pathSegment("repos", repository.getOwner(), repository.getName(), "branches")
                .queryParam("per_page", limit)
                .build()
                .toUri();
    }

    private URI buildIssueCommentsUri(PlatformConfigService.ResolvedPlatformConfig config, RepositoryConnection repository, int prNumber) {
        return baseBuilder(config).pathSegment("repos", repository.getOwner(), repository.getName(), "issues", String.valueOf(prNumber), "comments").build().toUri();
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
            String filename = firstText(file.get("filename"), file.get("path"), "");
            if (StringUtils.hasText(filename)) {
                result.add(filename);
            }
        }
        return result;
    }

    private String buildDiff(JsonNode files) {
        if (files == null || !files.isArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode file : files) {
            String filename = firstText(file.get("filename"), file.get("path"), "unknown");
            String patch = firstText(file.get("patch"), null, "");
            builder.append("### ").append(filename).append(System.lineSeparator());
            builder.append(StringUtils.hasText(patch) ? patch : "[No patch returned by GitHub API]");
            builder.append(System.lineSeparator()).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.get(fieldName) == null || node.get(fieldName).isNull()) {
            return "";
        }
        return node.get(fieldName).asText("");
    }

    private String firstText(JsonNode first, JsonNode second, String fallback) {
        String firstValue = textValue(first);
        if (StringUtils.hasText(firstValue)) {
            return firstValue;
        }
        String secondValue = textValue(second);
        if (StringUtils.hasText(secondValue)) {
            return secondValue;
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
