package com.patchlens.repository;

import com.patchlens.domain.RepositoryConnection;
import com.patchlens.domain.RepositoryProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryConnectionRepository extends JpaRepository<RepositoryConnection, Long> {

    Optional<RepositoryConnection> findByProviderAndOwnerAndName(
            RepositoryProvider provider,
            String owner,
            String name
    );
}
