package com.patchlens.repository;

import com.patchlens.domain.ReviewTask;
import com.patchlens.domain.ReviewStatus;
import com.patchlens.domain.ReviewTargetType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewTaskRepository extends JpaRepository<ReviewTask, Long> {

    Page<ReviewTask> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ReviewTask> findByStatusOrderByCreatedAtDesc(ReviewStatus status, Pageable pageable);

    List<ReviewTask> findTop50ByOrderByCreatedAtDesc();

    List<ReviewTask> findByRepositoryIdOrderByCreatedAtDesc(Long repositoryId);

    Optional<ReviewTask> findFirstByRepositoryIdAndTargetTypeAndCommitShaAndStatusInOrderByCreatedAtDesc(
            Long repositoryId,
            ReviewTargetType targetType,
            String commitSha,
            Collection<ReviewStatus> statuses
    );

    Optional<ReviewTask> findFirstByRepositoryIdAndTargetTypeAndPrNumberAndStatusInOrderByCreatedAtDesc(
            Long repositoryId,
            ReviewTargetType targetType,
            int prNumber,
            Collection<ReviewStatus> statuses
    );

    List<ReviewTask> findByStatusAndCreatedAtBefore(ReviewStatus status, Instant threshold);

    List<ReviewTask> findByStatusAndStartedAtBefore(ReviewStatus status, Instant threshold);

    long countByStatus(ReviewStatus status);

    long countByStatusAndCreatedAtBefore(ReviewStatus status, Instant threshold);

    long countByStatusAndStartedAtBefore(ReviewStatus status, Instant threshold);
}
