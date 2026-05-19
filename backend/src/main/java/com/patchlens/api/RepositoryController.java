package com.patchlens.api;

import com.patchlens.dto.CreateRepositoryRequest;
import com.patchlens.dto.CreateCommitReviewRequest;
import com.patchlens.dto.CreateReviewRequest;
import com.patchlens.dto.RepositoryBranchDto;
import com.patchlens.dto.RepositoryCommitDto;
import com.patchlens.dto.RepositoryDto;
import com.patchlens.dto.ReviewPreflightResponse;
import com.patchlens.dto.ReviewPolicyDto;
import com.patchlens.dto.ReviewTaskDto;
import com.patchlens.dto.UpdateReviewPolicyRequest;
import com.patchlens.service.RepositoryCommitService;
import com.patchlens.service.RepositoryService;
import com.patchlens.service.ReviewPreflightService;
import com.patchlens.service.ReviewService;
import com.patchlens.service.AuditLogService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repositories")
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final ReviewService reviewService;
    private final RepositoryCommitService repositoryCommitService;
    private final ReviewPreflightService reviewPreflightService;
    private final AuditLogService auditLogService;

    public RepositoryController(
            RepositoryService repositoryService,
            ReviewService reviewService,
            RepositoryCommitService repositoryCommitService,
            ReviewPreflightService reviewPreflightService,
            AuditLogService auditLogService
    ) {
        this.repositoryService = repositoryService;
        this.reviewService = reviewService;
        this.repositoryCommitService = repositoryCommitService;
        this.reviewPreflightService = reviewPreflightService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<RepositoryDto> listRepositories() {
        return repositoryService.listRepositories();
    }

    @PostMapping
    public RepositoryDto createRepository(@Valid @RequestBody CreateRepositoryRequest request) {
        RepositoryDto dto = repositoryService.createRepository(request);
        auditLogService.record("REPOSITORY_CREATE", "REPOSITORY", dto.id(), dto.owner() + "/" + dto.name());
        return dto;
    }

    @PostMapping("/{repositoryId}/enable")
    public RepositoryDto enableRepository(@PathVariable Long repositoryId) {
        RepositoryDto dto = repositoryService.setEnabled(repositoryId, true);
        auditLogService.record("REPOSITORY_ENABLE", "REPOSITORY", repositoryId, dto.owner() + "/" + dto.name());
        return dto;
    }

    @PostMapping("/{repositoryId}/disable")
    public RepositoryDto disableRepository(@PathVariable Long repositoryId) {
        RepositoryDto dto = repositoryService.setEnabled(repositoryId, false);
        auditLogService.record("REPOSITORY_DISABLE", "REPOSITORY", repositoryId, dto.owner() + "/" + dto.name());
        return dto;
    }

    @DeleteMapping("/{repositoryId}")
    public void deleteRepository(@PathVariable Long repositoryId) {
        repositoryService.deleteRepository(repositoryId);
        auditLogService.record("REPOSITORY_DELETE", "REPOSITORY", repositoryId, "删除仓库及关联审查记录");
    }

    @GetMapping("/{repositoryId}/commits")
    public List<RepositoryCommitDto> listRecentCommits(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String author,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return repositoryCommitService.listRecentCommits(repositoryId, branch, author, limit);
    }

    @GetMapping("/{repositoryId}/branches")
    public List<RepositoryBranchDto> listBranches(
            @PathVariable Long repositoryId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return repositoryCommitService.listBranches(repositoryId, limit);
    }

    @GetMapping("/{repositoryId}/policy")
    public ReviewPolicyDto getPolicy(@PathVariable Long repositoryId) {
        return repositoryService.getPolicy(repositoryId);
    }

    @PutMapping("/{repositoryId}/policy")
    public ReviewPolicyDto updatePolicy(
            @PathVariable Long repositoryId,
            @Valid @RequestBody UpdateReviewPolicyRequest request
    ) {
        ReviewPolicyDto dto = repositoryService.updatePolicy(repositoryId, request);
        auditLogService.record("REVIEW_POLICY_UPDATE", "REPOSITORY", repositoryId, "更新质量控制策略");
        return dto;
    }

    @PostMapping("/{repositoryId}/reviews")
    public ReviewTaskDto createManualReview(
            @PathVariable Long repositoryId,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        ReviewTaskDto dto = reviewService.createManualReview(repositoryId, request);
        auditLogService.record("REVIEW_CREATE_PR", "REVIEW", dto.id(), dto.repositoryName());
        return dto;
    }

    @PostMapping("/{repositoryId}/commit-reviews")
    public ReviewTaskDto createCommitReview(
            @PathVariable Long repositoryId,
            @Valid @RequestBody CreateCommitReviewRequest request
    ) {
        ReviewTaskDto dto = reviewService.createCommitReview(repositoryId, request);
        auditLogService.record("REVIEW_CREATE_COMMIT", "REVIEW", dto.id(), dto.repositoryName());
        return dto;
    }

    @PostMapping("/{repositoryId}/commit-reviews/preflight")
    public ReviewPreflightResponse preflightCommitReview(
            @PathVariable Long repositoryId,
            @Valid @RequestBody CreateCommitReviewRequest request
    ) {
        return reviewPreflightService.preflightCommitReview(repositoryId, request);
    }
}
