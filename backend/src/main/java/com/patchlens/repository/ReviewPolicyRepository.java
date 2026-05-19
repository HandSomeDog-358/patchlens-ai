package com.patchlens.repository;

import com.patchlens.domain.ReviewPolicy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewPolicyRepository extends JpaRepository<ReviewPolicy, Long> {

    Optional<ReviewPolicy> findByRepositoryId(Long repositoryId);
}
