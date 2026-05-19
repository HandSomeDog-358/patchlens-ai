package com.patchlens.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patchlens.domain.RepositoryProvider;
import com.patchlens.dto.ReviewTaskDto;
import com.patchlens.service.AuditLogService;
import com.patchlens.service.ReviewService;
import com.patchlens.service.WebhookSecurityService;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/gitea")
public class GiteaWebhookController {

    private final ObjectMapper objectMapper;
    private final ReviewService reviewService;
    private final WebhookSecurityService webhookSecurityService;
    private final AuditLogService auditLogService;

    public GiteaWebhookController(
            ObjectMapper objectMapper,
            ReviewService reviewService,
            WebhookSecurityService webhookSecurityService,
            AuditLogService auditLogService
    ) {
        this.objectMapper = objectMapper;
        this.reviewService = reviewService;
        this.webhookSecurityService = webhookSecurityService;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(name = "X-Gitea-Event", required = false) String event,
            @RequestHeader Map<String, String> headers,
            @RequestBody String payload
    ) throws IOException {
        webhookSecurityService.validate(RepositoryProvider.GITEA, headers, payload);
        JsonNode root = objectMapper.readTree(payload);
        WebhookPullRequest pullRequest = parsePullRequest(root);
        if (pullRequest == null) {
            return ResponseEntity.accepted().body(Map.of(
                    "status", "ignored",
                    "event", event == null ? "unknown" : event
            ));
        }

        ReviewTaskDto review = reviewService.createWebhookReview(
                RepositoryProvider.GITEA,
                pullRequest.owner(),
                pullRequest.repositoryName(),
                pullRequest.defaultBranch(),
                pullRequest.prNumber(),
                pullRequest.commitSha()
        );
        auditLogService.record("REVIEW_CREATE_WEBHOOK", "REVIEW", review.id(), "Gitea " + review.repositoryName());

        return ResponseEntity.accepted().body(Map.of(
                "status", "accepted",
                "event", event == null ? "unknown" : event,
                "reviewId", review.id()
        ));
    }

    private WebhookPullRequest parsePullRequest(JsonNode root) {
        int prNumber = firstInt(root.at("/pull_request/number"), root.at("/number"));
        if (prNumber <= 0) {
            return null;
        }

        String fullName = firstText(root.at("/repository/full_name"), root.at("/repository/name"));
        String owner = firstText(root.at("/repository/owner/login"), root.at("/repository/owner/username"));
        String repositoryName = firstText(root.at("/repository/name"), root.at("/repository/repo_name"));

        if (!StringUtils.hasText(owner) || !StringUtils.hasText(repositoryName)) {
            String[] parts = splitFullName(fullName);
            owner = parts[0];
            repositoryName = parts[1];
        }
        if (!StringUtils.hasText(owner) || !StringUtils.hasText(repositoryName)) {
            return null;
        }

        String defaultBranch = firstText(root.at("/repository/default_branch"), root.at("/repository/default_branch_name"));
        if (!StringUtils.hasText(defaultBranch)) {
            defaultBranch = "main";
        }

        String commitSha = firstText(root.at("/pull_request/head/sha"), root.at("/pull_request/head/ref"));
        if (!StringUtils.hasText(commitSha)) {
            commitSha = "webhook";
        }

        return new WebhookPullRequest(owner, repositoryName, defaultBranch, prNumber, commitSha);
    }

    private String[] splitFullName(String fullName) {
        if (!StringUtils.hasText(fullName) || !fullName.contains("/")) {
            return new String[] {"", ""};
        }
        int slashIndex = fullName.indexOf('/');
        return new String[] {fullName.substring(0, slashIndex), fullName.substring(slashIndex + 1)};
    }

    private String firstText(JsonNode first, JsonNode second) {
        String value = text(first);
        if (StringUtils.hasText(value)) {
            return value;
        }
        value = text(second);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return "";
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.asText("");
    }

    private int firstInt(JsonNode first, JsonNode second) {
        for (JsonNode node : new JsonNode[] {first, second}) {
            if (node != null && !node.isMissingNode() && node.canConvertToInt()) {
                return node.asInt();
            }
        }
        return 0;
    }

    private record WebhookPullRequest(
            String owner,
            String repositoryName,
            String defaultBranch,
            int prNumber,
            String commitSha
    ) {
    }
}
