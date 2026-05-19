package com.patchlens.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.patchlens.domain.RepositoryConnection;
import com.patchlens.domain.RepositoryProvider;
import com.patchlens.dto.RepositoryBranchDto;
import com.patchlens.dto.RepositoryCommitDto;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GiteePullRequestClient {

    private final PlatformConfigService platformConfigService;
    private final RestClient restClient;

    public GiteePullRequestClient(PlatformConfigService platformConfigService, RestClient.Builder restClientBuilder) {
        this.platformConfigService = platformConfigService;
        this.restClient = restClientBuilder.build();
    }

    public boolean isConfigured() {
        return platformConfigService.resolve(RepositoryProvider.GITEE)
                .map(PlatformConfigService.ResolvedPlatformConfig::hasApiAccess)
                .orElse(false);
    }

    public PullRequestContext fetch(RepositoryConnection repository, int prNumber, String fallbackCommitSha) {
        PlatformConfigService.ResolvedPlatformConfig config = platformConfigService.resolve(RepositoryProvider.GITEE)
                .filter(PlatformConfigService.ResolvedPlatformConfig::hasApiAccess)
                .orElseThrow(() -> new IllegalStateException("Gitee platform config is not ready"));

        JsonNode pr = restClient.get()
                .uri(buildPullRequestUri(config, repository, prNumber))
                .retrieve()
                .body(JsonNode.class);

        JsonNode files = restClient.get()
                .uri(buildPullRequestFilesUri(config, repository, prNumber))
                .retrieve()
                .body(JsonNode.class);

        String title = text(pr, "title");
        String description = firstText(pr, "body", "description");
        String commitSha = firstText(pr.at("/head/sha"), pr.at("/head/ref"), fallbackCommitSha);
        List<String> changedFiles = extractChangedFiles(files);
        String diff = buildDiff(files);

        return new PullRequestContext(title, description, commitSha, diff, changedFiles);
    }

    public PullRequestContext fetchCommit(RepositoryConnection repository, String commitSha) {
        PlatformConfigService.ResolvedPlatformConfig config = platformConfigService.resolve(RepositoryProvider.GITEE)
                .filter(PlatformConfigService.ResolvedPlatformConfig::hasApiAccess)
                .orElseThrow(() -> new IllegalStateException("Gitee platform config is not ready"));

        JsonNode commit = restClient.get()
                .uri(buildCommitUri(config, repository, commitSha))
                .retrieve()
                .body(JsonNode.class);

        JsonNode files = commit == null ? null : commit.get("files");
        String title = firstText(
                commit == null ? null : commit.at("/commit/message"),
                commit == null ? null : commit.get("message"),
                "Commit " + commitSha
        );
        String description = firstText(
                commit == null ? null : commit.at("/commit/committer/name"),
                commit == null ? null : commit.at("/committer/name"),
                ""
        );
        List<String> changedFiles = extractChangedFiles(files);
        String diff = buildDiff(files);

        return new PullRequestContext(title, description, commitSha, diff, changedFiles);
    }

    public List<RepositoryCommitDto> listRecentCommits(RepositoryConnection repository, String branch, String author, int limit) {
        PlatformConfigService.ResolvedPlatformConfig config = platformConfigService.resolve(RepositoryProvider.GITEE)
                .filter(PlatformConfigService.ResolvedPlatformConfig::hasApiAccess)
                .orElseThrow(() -> new IllegalStateException("Gitee platform config is not ready"));

        JsonNode commits = restClient.get()
                .uri(buildCommitsUri(config, repository, branch, author, limit))
                .retrieve()
                .body(JsonNode.class);

        List<RepositoryCommitDto> result = new ArrayList<>();
        if (commits == null || !commits.isArray()) {
            return result;
        }
        for (JsonNode commit : commits) {
            String sha = firstText(commit.get("sha"), commit.get("id"), "");
            String message = firstLine(firstText(commit.at("/commit/message"), commit.get("message"), "无提交信息"));
            String authorName = firstText(commit.at("/commit/author/name"), commit.at("/author/name"), "");
            String authoredAt = firstText(commit.at("/commit/author/date"), commit.at("/author/date"), "");
            String webUrl = firstText(commit.get("html_url"), commit.get("url"), "");
            if (StringUtils.hasText(sha)) {
                result.add(RepositoryCommitDto.of(sha, message, authorName, authoredAt, webUrl));
            }
        }
        return result;
    }

    public List<RepositoryBranchDto> listBranches(RepositoryConnection repository, int limit) {
        PlatformConfigService.ResolvedPlatformConfig config = platformConfigService.resolve(RepositoryProvider.GITEE)
                .filter(PlatformConfigService.ResolvedPlatformConfig::hasApiAccess)
                .orElseThrow(() -> new IllegalStateException("Gitee platform config is not ready"));

        JsonNode branches = restClient.get()
                .uri(buildBranchesUri(config, repository, limit))
                .retrieve()
                .body(JsonNode.class);

        List<RepositoryBranchDto> result = new ArrayList<>();
        if (branches == null || !branches.isArray()) {
            return result;
        }
        for (JsonNode branch : branches) {
            String name = firstText(branch.get("name"), branch.get("branch_name"), "");
            String sha = firstText(branch.at("/commit/sha"), branch.at("/commit/id"), "");
            if (StringUtils.hasText(name)) {
                result.add(RepositoryBranchDto.of(name, sha));
            }
        }
        return result;
    }

    public String createPullRequestComment(RepositoryConnection repository, int prNumber, String body) {
        PlatformConfigService.ResolvedPlatformConfig config = platformConfigService.resolve(RepositoryProvider.GITEE)
                .filter(PlatformConfigService.ResolvedPlatformConfig::hasApiAccess)
                .orElseThrow(() -> new IllegalStateException("Gitee platform config is not ready"));
        JsonNode response = restClient.post()
                .uri(buildPullRequestCommentsUri(config, repository, prNumber))
                .body(Map.of("body", body))
                .retrieve()
                .body(JsonNode.class);
        return firstText(response == null ? null : response.get("id"), response == null ? null : response.get("url"), "");
    }

    private URI buildPullRequestUri(
            PlatformConfigService.ResolvedPlatformConfig config,
            RepositoryConnection repository,
            int prNumber
    ) {
        return baseBuilder(config)
                .pathSegment("repos", repository.getOwner(), repository.getName(), "pulls", String.valueOf(prNumber))
                .queryParam("access_token", config.accessToken())
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
                .queryParam("access_token", config.accessToken())
                .build()
                .toUri();
    }

    private URI buildPullRequestCommentsUri(
            PlatformConfigService.ResolvedPlatformConfig config,
            RepositoryConnection repository,
            int prNumber
    ) {
        return baseBuilder(config)
                .pathSegment("repos", repository.getOwner(), repository.getName(), "pulls", String.valueOf(prNumber), "comments")
                .queryParam("access_token", config.accessToken())
                .build()
                .toUri();
    }

    private URI buildCommitUri(
            PlatformConfigService.ResolvedPlatformConfig config,
            RepositoryConnection repository,
            String commitSha
    ) {
        return baseBuilder(config)
                .pathSegment("repos", repository.getOwner(), repository.getName(), "commits", commitSha)
                .queryParam("access_token", config.accessToken())
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
                .queryParam("per_page", limit)
                .queryParam("access_token", config.accessToken());
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
                .queryParam("per_page", limit)
                .queryParam("access_token", config.accessToken())
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
            String filename = firstText(file.get("filename"), file.get("new_path"), file.get("path"), "");
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
            String filename = firstText(file.get("filename"), file.get("new_path"), file.get("path"), "unknown");
            String patch = firstText(file.get("patch"), file.get("diff"), "");
            builder.append("### ").append(filename).append(System.lineSeparator());
            if (StringUtils.hasText(patch)) {
                builder.append(patch);
            } else {
                builder.append("[No patch returned by Gitee API]");
            }
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
        if (node == null || node.isNull()) {
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

    private String firstText(JsonNode node, String firstField, String secondField) {
        return firstText(node == null ? null : node.get(firstField), node == null ? null : node.get(secondField), "");
    }
}
