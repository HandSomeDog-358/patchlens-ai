package com.patchlens.repository;

import com.patchlens.domain.PlatformConfig;
import com.patchlens.domain.RepositoryProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformConfigRepository extends JpaRepository<PlatformConfig, Long> {

    Optional<PlatformConfig> findFirstByProviderAndEnabledTrueOrderByUpdatedAtDesc(RepositoryProvider provider);

    boolean existsByProvider(RepositoryProvider provider);
}
