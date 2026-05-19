package com.patchlens.repository;

import com.patchlens.domain.ReviewFeedback;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewFeedbackRepository extends JpaRepository<ReviewFeedback, Long> {

    List<ReviewFeedback> findByFindingIdInOrderByCreatedAtDesc(Collection<Long> findingIds);
}
