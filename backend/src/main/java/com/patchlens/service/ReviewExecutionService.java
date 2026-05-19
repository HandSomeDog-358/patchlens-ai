package com.patchlens.service;

import com.patchlens.domain.ReviewFinding;
import com.patchlens.domain.ReviewConclusion;
import com.patchlens.domain.ReviewStatus;
import com.patchlens.domain.ReviewTask;
import com.patchlens.domain.ReviewTargetType;
import com.patchlens.repository.ReviewFindingRepository;
import com.patchlens.repository.ReviewPolicyRepository;
import com.patchlens.repository.ReviewTaskRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ReviewExecutionService {

    private final ReviewTaskRepository reviewTaskRepository;
    private final ReviewFindingRepository reviewFindingRepository;
    private final ReviewPolicyRepository reviewPolicyRepository;
    private final AiReviewClient aiReviewClient;
    private final PullRequestContextLoader pullRequestContextLoader;
    private final ReviewPolicyApplier reviewPolicyApplier;
    private final PullRequestCommentService pullRequestCommentService;
    private final ReviewCommentFormatter reviewCommentFormatter;
    private final TransactionTemplate transactionTemplate;

    public ReviewExecutionService(
            ReviewTaskRepository reviewTaskRepository,
            ReviewFindingRepository reviewFindingRepository,
            ReviewPolicyRepository reviewPolicyRepository,
            AiReviewClient aiReviewClient,
            PullRequestContextLoader pullRequestContextLoader,
            ReviewPolicyApplier reviewPolicyApplier,
            PullRequestCommentService pullRequestCommentService,
            ReviewCommentFormatter reviewCommentFormatter,
            TransactionTemplate transactionTemplate
    ) {
        this.reviewTaskRepository = reviewTaskRepository;
        this.reviewFindingRepository = reviewFindingRepository;
        this.reviewPolicyRepository = reviewPolicyRepository;
        this.aiReviewClient = aiReviewClient;
        this.pullRequestContextLoader = pullRequestContextLoader;
        this.reviewPolicyApplier = reviewPolicyApplier;
        this.pullRequestCommentService = pullRequestCommentService;
        this.reviewCommentFormatter = reviewCommentFormatter;
        this.transactionTemplate = transactionTemplate;
    }

    public void runReview(Long reviewId) {
        ReviewJob job = transactionTemplate.execute(status -> {
            ReviewTask task = getReviewTask(reviewId);
            if (task.getStatus() != ReviewStatus.QUEUED) {
                return null;
            }
            task.setStatus(ReviewStatus.RUNNING);
            task.setStartedAt(Instant.now());
            task.setFinishedAt(null);
            task.setErrorMessage(null);
            task.setSummary(null);
            reviewFindingRepository.deleteByReviewTaskId(reviewId);

            var repository = task.getRepository();
            repository.getOwner();
            repository.getName();
            repository.getDefaultBranch();
            return new ReviewJob(
                    reviewId,
                    repository,
                    task.getTargetType(),
                    task.getPrNumber(),
                    task.getCommitSha(),
                    reviewPolicyRepository.findByRepositoryId(repository.getId())
                            .map(ReviewPolicySnapshot::from)
                            .orElseGet(ReviewPolicySnapshot::defaults)
            );
        });
        if (job == null) {
            return;
        }

        try {
            PullRequestContext context = job.targetType() == ReviewTargetType.COMMIT
                    ? pullRequestContextLoader.loadCommit(job.repository(), job.commitSha())
                    : pullRequestContextLoader.load(job.repository(), job.prNumber(), job.commitSha());
            String effectiveCommitSha = context.commitSha() != null && !context.commitSha().isBlank()
                    ? context.commitSha()
                    : job.commitSha();

            AiReviewClient.ReviewInput input = new AiReviewClient.ReviewInput(
                    job.repository().getOwner() + "/" + job.repository().getName(),
                    job.targetType().name(),
                    targetRef(job.targetType(), job.prNumber(), effectiveCommitSha),
                    job.prNumber(),
                    effectiveCommitSha,
                    context.title(),
                    context.description(),
                    context.diff(),
                    context.changedFiles(),
                    job.policy().language(),
                    job.policy().minConfidence(),
                    job.policy().maxInlineComments(),
                    job.policy().enableSummary(),
                    job.policy().enableInlineComments(),
                    job.policy().enableSuggestedPatch(),
                    job.policy().ignoredPaths(),
                    job.policy().focusPaths()
            );
            AiReviewClient.ReviewResult result = aiReviewClient.review(input);

            List<AiReviewClient.FindingCandidate> candidates = reviewPolicyApplier.apply(result.findings(), job.policy());
            ReviewConclusion conclusion = reviewPolicyApplier.conclude(candidates);

            String summary = job.policy().enableSummary()
                    ? result.summary()
                    : "审查摘要已按质量策略关闭。";
            transactionTemplate.executeWithoutResult(status -> completeReview(
                    job.reviewId(),
                    effectiveCommitSha,
                    summary,
                    candidates,
                    conclusion
            ));
            publishReviewComment(job, effectiveCommitSha, summary, candidates, conclusion);
        } catch (RuntimeException ex) {
            transactionTemplate.executeWithoutResult(status -> failReview(job.reviewId(), ex));
        }
    }

    private void completeReview(
            Long reviewId,
            String effectiveCommitSha,
            String summary,
            List<AiReviewClient.FindingCandidate> candidates,
            ReviewConclusion conclusion
    ) {
        ReviewTask task = getReviewTask(reviewId);
        if (task.getStatus() == ReviewStatus.CANCELED) {
            return;
        }
        task.setCommitSha(effectiveCommitSha);
        task.setSummary(summary);
        task.setConclusion(conclusion);
        task.setPublishError(null);

        for (AiReviewClient.FindingCandidate candidate : candidates) {
            ReviewFinding finding = new ReviewFinding();
            finding.setReviewTask(task);
            finding.setSeverity(candidate.severity());
            finding.setConfidence(candidate.confidence());
            finding.setFilePath(candidate.filePath());
            finding.setLineNumber(candidate.lineNumber());
            finding.setTitle(candidate.title());
            finding.setDescription(candidate.description());
            finding.setEvidence(candidate.evidence());
            finding.setSuggestion(candidate.suggestion());
            finding.setPatch(candidate.patch());
            finding.setPublished(false);
            reviewFindingRepository.save(finding);
        }

        task.setStatus(ReviewStatus.COMPLETED);
        task.setFinishedAt(Instant.now());
    }

    private void publishReviewComment(
            ReviewJob job,
            String effectiveCommitSha,
            String summary,
            List<AiReviewClient.FindingCandidate> candidates,
            ReviewConclusion conclusion
    ) {
        if (job.targetType() != ReviewTargetType.PULL_REQUEST) {
            return;
        }
        try {
            String body = reviewCommentFormatter.format(
                    job.repository().getOwner() + "/" + job.repository().getName(),
                    "#" + job.prNumber(),
                    effectiveCommitSha,
                    conclusion,
                    summary,
                    candidates
            );
            String providerCommentId = pullRequestCommentService.publishSummary(job.repository(), job.prNumber(), body);
            transactionTemplate.executeWithoutResult(status -> markPublished(job.reviewId(), providerCommentId));
        } catch (RuntimeException ex) {
            transactionTemplate.executeWithoutResult(status -> markPublishFailed(job.reviewId(), ex));
        }
    }

    private void markPublished(Long reviewId, String providerCommentId) {
        ReviewTask task = getReviewTask(reviewId);
        if (task.getStatus() == ReviewStatus.CANCELED) {
            return;
        }
        task.setPublishedAt(Instant.now());
        task.setPublishError(null);
        for (ReviewFinding finding : reviewFindingRepository.findByReviewTaskIdOrderBySeverityAscConfidenceDesc(reviewId)) {
            finding.setPublished(true);
            finding.setProviderCommentId(providerCommentId);
        }
    }

    private void markPublishFailed(Long reviewId, RuntimeException ex) {
        ReviewTask task = getReviewTask(reviewId);
        if (task.getStatus() == ReviewStatus.CANCELED) {
            return;
        }
        task.setPublishError(ex.getMessage());
    }

    private void failReview(Long reviewId, RuntimeException ex) {
        ReviewTask task = getReviewTask(reviewId);
        if (task.getStatus() == ReviewStatus.CANCELED) {
            return;
        }
        task.setStatus(ReviewStatus.FAILED);
        task.setConclusion(null);
        task.setErrorMessage(ex.getMessage());
        task.setFinishedAt(Instant.now());
    }

    private ReviewTask getReviewTask(Long reviewId) {
        return reviewTaskRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review task not found"));
    }

    private String targetRef(ReviewTargetType targetType, int prNumber, String commitSha) {
        if (targetType == ReviewTargetType.COMMIT) {
            return commitSha;
        }
        return "#" + prNumber;
    }

    private record ReviewJob(
            Long reviewId,
            com.patchlens.domain.RepositoryConnection repository,
            ReviewTargetType targetType,
            int prNumber,
            String commitSha,
            ReviewPolicySnapshot policy
    ) {
    }
}
