package com.patchlens.service;

import com.patchlens.domain.RepositoryConnection;
import com.patchlens.domain.RepositoryProvider;
import com.patchlens.domain.ReviewFeedback;
import com.patchlens.domain.ReviewStatus;
import com.patchlens.domain.ReviewTask;
import com.patchlens.domain.ReviewTargetType;
import com.patchlens.domain.TriggerType;
import com.patchlens.dto.CreateCommitReviewRequest;
import com.patchlens.dto.CreateReviewRequest;
import com.patchlens.dto.PageResponse;
import com.patchlens.dto.ReviewGovernanceDto;
import com.patchlens.dto.ReviewFindingDto;
import com.patchlens.dto.ReviewTaskDto;
import com.patchlens.repository.ReviewFeedbackRepository;
import com.patchlens.repository.ReviewFindingRepository;
import com.patchlens.repository.ReviewTaskRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class ReviewService {

    private static final List<ReviewStatus> ACTIVE_STATUSES = List.of(ReviewStatus.QUEUED, ReviewStatus.RUNNING);

    private final RepositoryService repositoryService;
    private final ReviewTaskRepository reviewTaskRepository;
    private final ReviewFindingRepository reviewFindingRepository;
    private final ReviewFeedbackRepository reviewFeedbackRepository;
    private final ReviewExecutionService reviewExecutionService;
    private final TaskExecutor taskExecutor;

    public ReviewService(
            RepositoryService repositoryService,
            ReviewTaskRepository reviewTaskRepository,
            ReviewFindingRepository reviewFindingRepository,
            ReviewFeedbackRepository reviewFeedbackRepository,
            ReviewExecutionService reviewExecutionService,
            TaskExecutor taskExecutor
    ) {
        this.repositoryService = repositoryService;
        this.reviewTaskRepository = reviewTaskRepository;
        this.reviewFindingRepository = reviewFindingRepository;
        this.reviewFeedbackRepository = reviewFeedbackRepository;
        this.reviewExecutionService = reviewExecutionService;
        this.taskExecutor = taskExecutor;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewTaskDto> listReviews(int page, int size, String status) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        Page<ReviewTask> tasks;
        if (StringUtils.hasText(status)) {
            ReviewStatus reviewStatus = parseStatus(status);
            tasks = reviewTaskRepository.findByStatusOrderByCreatedAtDesc(reviewStatus, pageable);
        } else {
            tasks = reviewTaskRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return PageResponse.from(tasks.map(ReviewTaskDto::from));
    }

    @Transactional(readOnly = true)
    public ReviewGovernanceDto governance(int stuckMinutes) {
        int minutes = Math.max(1, Math.min(stuckMinutes, 24 * 60));
        Instant threshold = Instant.now().minus(Duration.ofMinutes(minutes));
        long queued = reviewTaskRepository.countByStatus(ReviewStatus.QUEUED);
        long running = reviewTaskRepository.countByStatus(ReviewStatus.RUNNING);
        long completed = reviewTaskRepository.countByStatus(ReviewStatus.COMPLETED);
        long failed = reviewTaskRepository.countByStatus(ReviewStatus.FAILED);
        long canceled = reviewTaskRepository.countByStatus(ReviewStatus.CANCELED);
        long stuck = reviewTaskRepository.countByStatusAndCreatedAtBefore(ReviewStatus.QUEUED, threshold)
                + reviewTaskRepository.countByStatusAndStartedAtBefore(ReviewStatus.RUNNING, threshold);
        return new ReviewGovernanceDto(queued, running, completed, failed, canceled, stuck, minutes);
    }

    @Transactional(readOnly = true)
    public ReviewTaskDto getReview(Long reviewId) {
        return ReviewTaskDto.from(getReviewTask(reviewId));
    }

    @Transactional(readOnly = true)
    public List<ReviewFindingDto> listFindings(Long reviewId) {
        var findings = reviewFindingRepository.findByReviewTaskIdOrderBySeverityAscConfidenceDesc(reviewId);
        if (findings.isEmpty()) {
            return List.of();
        }
        List<Long> findingIds = findings.stream().map(finding -> finding.getId()).toList();
        Map<Long, List<ReviewFeedback>> feedbacksByFinding = reviewFeedbackRepository
                .findByFindingIdInOrderByCreatedAtDesc(findingIds)
                .stream()
                .collect(Collectors.groupingBy(feedback -> feedback.getFinding().getId()));
        return findings.stream()
                .map(finding -> ReviewFindingDto.from(finding, feedbacksByFinding.getOrDefault(finding.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ReviewTaskDto createManualReview(Long repositoryId, CreateReviewRequest request) {
        RepositoryConnection repository = repositoryService.getRepository(repositoryId);
        return createReview(repository, ReviewTargetType.PULL_REQUEST, request.prNumber(), request.commitSha(), TriggerType.MANUAL);
    }

    @Transactional
    public ReviewTaskDto createCommitReview(Long repositoryId, CreateCommitReviewRequest request) {
        RepositoryConnection repository = repositoryService.getRepository(repositoryId);
        return createReview(repository, ReviewTargetType.COMMIT, 0, request.commitSha(), TriggerType.MANUAL);
    }

    @Transactional
    public ReviewTaskDto createWebhookReview(
            RepositoryProvider provider,
            String owner,
            String name,
            String defaultBranch,
            int prNumber,
            String commitSha
    ) {
        RepositoryConnection repository = repositoryService.findOrCreate(provider, owner, name, defaultBranch);
        return createReview(repository, ReviewTargetType.PULL_REQUEST, prNumber, commitSha, TriggerType.WEBHOOK);
    }

    private ReviewTaskDto createReview(
            RepositoryConnection repository,
            ReviewTargetType targetType,
            int prNumber,
            String commitSha,
            TriggerType triggerType
    ) {
        Optional<ReviewTask> existing = findActiveDuplicate(repository, targetType, prNumber, commitSha);
        if (existing.isPresent()) {
            return ReviewTaskDto.from(existing.get());
        }

        ReviewTask task = new ReviewTask();
        task.setRepository(repository);
        task.setProvider(repository.getProvider());
        task.setTargetType(targetType);
        task.setPrNumber(prNumber);
        task.setCommitSha(commitSha);
        task.setTriggerType(triggerType);
        ReviewTask saved = reviewTaskRepository.save(task);
        scheduleReview(saved.getId());
        return ReviewTaskDto.from(saved);
    }

    @Transactional
    public ReviewTaskDto rerun(Long reviewId) {
        ReviewTask task = getReviewTask(reviewId);
        task.setStatus(ReviewStatus.QUEUED);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        task.setErrorMessage(null);
        task.setSummary(null);
        task.setConclusion(null);
        task.setPublishedAt(null);
        task.setPublishError(null);
        reviewFindingRepository.deleteByReviewTaskId(reviewId);
        scheduleReview(reviewId);
        return ReviewTaskDto.from(task);
    }

    @Transactional
    public ReviewTaskDto cancel(Long reviewId) {
        ReviewTask task = getReviewTask(reviewId);
        if (task.getStatus() == ReviewStatus.QUEUED || task.getStatus() == ReviewStatus.RUNNING) {
            task.setStatus(ReviewStatus.CANCELED);
            task.setConclusion(null);
            task.setErrorMessage("用户取消审查");
            task.setFinishedAt(Instant.now());
        }
        return ReviewTaskDto.from(task);
    }

    @Transactional
    public List<ReviewTaskDto> cleanupStuck(int minutes) {
        Instant threshold = Instant.now().minus(Duration.ofMinutes(Math.max(1, minutes)));
        List<ReviewTask> stuck = new ArrayList<>();
        stuck.addAll(reviewTaskRepository.findByStatusAndCreatedAtBefore(ReviewStatus.QUEUED, threshold));
        stuck.addAll(reviewTaskRepository.findByStatusAndStartedAtBefore(ReviewStatus.RUNNING, threshold));

        for (ReviewTask task : stuck) {
            task.setStatus(ReviewStatus.FAILED);
            task.setErrorMessage("任务超过 " + Math.max(1, minutes) + " 分钟未完成，已自动清理");
            task.setFinishedAt(Instant.now());
        }
        return stuck.stream().map(ReviewTaskDto::from).toList();
    }

    private ReviewTask getReviewTask(Long reviewId) {
        return reviewTaskRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review task not found"));
    }

    private ReviewStatus parseStatus(String status) {
        try {
            return ReviewStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("不支持的审查状态：" + status);
        }
    }

    private Optional<ReviewTask> findActiveDuplicate(
            RepositoryConnection repository,
            ReviewTargetType targetType,
            int prNumber,
            String commitSha
    ) {
        if (targetType == ReviewTargetType.COMMIT) {
            return reviewTaskRepository.findFirstByRepositoryIdAndTargetTypeAndCommitShaAndStatusInOrderByCreatedAtDesc(
                    repository.getId(),
                    targetType,
                    commitSha,
                    ACTIVE_STATUSES
            );
        }
        return reviewTaskRepository.findFirstByRepositoryIdAndTargetTypeAndPrNumberAndStatusInOrderByCreatedAtDesc(
                repository.getId(),
                targetType,
                prNumber,
                ACTIVE_STATUSES
        );
    }

    private void scheduleReview(Long reviewId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    taskExecutor.execute(() -> reviewExecutionService.runReview(reviewId));
                }
            });
            return;
        }
        taskExecutor.execute(() -> reviewExecutionService.runReview(reviewId));
    }
}
