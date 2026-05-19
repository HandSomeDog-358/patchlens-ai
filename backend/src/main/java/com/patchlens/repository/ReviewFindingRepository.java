package com.patchlens.repository;

import com.patchlens.domain.ReviewFinding;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewFindingRepository extends JpaRepository<ReviewFinding, Long> {

    List<ReviewFinding> findByReviewTaskIdOrderBySeverityAscConfidenceDesc(Long reviewTaskId);

    void deleteByReviewTaskId(Long reviewTaskId);
}
