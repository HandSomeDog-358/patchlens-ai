package com.patchlens.repository;

import com.patchlens.domain.ModelConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelConfigRepository extends JpaRepository<ModelConfig, Long> {

    List<ModelConfig> findByEnabledTrue();

    Optional<ModelConfig> findFirstByEnabledTrueOrderByUpdatedAtDescIdDesc();

    List<ModelConfig> findByIdNotAndEnabledTrue(Long id);
}
