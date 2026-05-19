package com.patchlens.service;

import com.patchlens.domain.RepositoryConnection;
import com.patchlens.domain.RepositoryProvider;
import com.patchlens.domain.ReviewPolicy;
import com.patchlens.dto.CreateRepositoryRequest;
import com.patchlens.dto.RepositoryDto;
import com.patchlens.dto.ReviewPolicyDto;
import com.patchlens.dto.UpdateReviewPolicyRequest;
import com.patchlens.repository.RepositoryConnectionRepository;
import com.patchlens.repository.ReviewPolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepositoryService {

    private final RepositoryConnectionRepository repositoryConnectionRepository;
    private final ReviewPolicyRepository reviewPolicyRepository;

    public RepositoryService(
            RepositoryConnectionRepository repositoryConnectionRepository,
            ReviewPolicyRepository reviewPolicyRepository
    ) {
        this.repositoryConnectionRepository = repositoryConnectionRepository;
        this.reviewPolicyRepository = reviewPolicyRepository;
    }

    @Transactional(readOnly = true)
    public List<RepositoryDto> listRepositories() {
        return repositoryConnectionRepository.findAll().stream()
                .map(RepositoryDto::from)
                .toList();
    }

    @Transactional
    public RepositoryDto createRepository(CreateRepositoryRequest request) {
        RepositoryProvider provider = RepositoryProvider.valueOf(request.provider().toUpperCase());
        RepositoryConnection repository = new RepositoryConnection();
        repository.setProvider(provider);
        repository.setOwner(request.owner());
        repository.setName(request.name());
        if (request.defaultBranch() != null && !request.defaultBranch().isBlank()) {
            repository.setDefaultBranch(request.defaultBranch());
        }

        RepositoryConnection saved = repositoryConnectionRepository.save(repository);
        ReviewPolicy policy = new ReviewPolicy();
        policy.setRepository(saved);
        reviewPolicyRepository.save(policy);
        return RepositoryDto.from(saved);
    }

    @Transactional
    public RepositoryConnection findOrCreate(
            RepositoryProvider provider,
            String owner,
            String name,
            String defaultBranch
    ) {
        return repositoryConnectionRepository.findByProviderAndOwnerAndName(provider, owner, name)
                .orElseGet(() -> {
                    RepositoryConnection repository = new RepositoryConnection();
                    repository.setProvider(provider);
                    repository.setOwner(owner);
                    repository.setName(name);
                    if (defaultBranch != null && !defaultBranch.isBlank()) {
                        repository.setDefaultBranch(defaultBranch);
                    }
                    RepositoryConnection saved = repositoryConnectionRepository.save(repository);
                    ReviewPolicy policy = new ReviewPolicy();
                    policy.setRepository(saved);
                    reviewPolicyRepository.save(policy);
                    return saved;
                });
    }

    @Transactional
    public RepositoryDto setEnabled(Long repositoryId, boolean enabled) {
        RepositoryConnection repository = getRepository(repositoryId);
        repository.setEnabled(enabled);
        repository.markUpdated();
        return RepositoryDto.from(repository);
    }

    @Transactional
    public void deleteRepository(Long repositoryId) {
        RepositoryConnection repository = getRepository(repositoryId);
        repositoryConnectionRepository.delete(repository);
    }

    @Transactional(readOnly = true)
    public ReviewPolicyDto getPolicy(Long repositoryId) {
        ReviewPolicy policy = reviewPolicyRepository.findByRepositoryId(repositoryId)
                .orElseThrow(() -> new EntityNotFoundException("Review policy not found"));
        return ReviewPolicyDto.from(policy);
    }

    @Transactional
    public ReviewPolicyDto updatePolicy(Long repositoryId, UpdateReviewPolicyRequest request) {
        ReviewPolicy policy = reviewPolicyRepository.findByRepositoryId(repositoryId)
                .orElseThrow(() -> new EntityNotFoundException("Review policy not found"));
        policy.setLanguage(request.language());
        policy.setMinConfidence(request.minConfidence());
        policy.setMaxInlineComments(request.maxInlineComments());
        policy.setEnableSummary(request.enableSummary());
        policy.setEnableInlineComments(request.enableInlineComments());
        policy.setEnableSuggestedPatch(request.enableSuggestedPatch());
        policy.setIgnoredPaths(request.ignoredPaths());
        policy.setFocusPaths(request.focusPaths());
        policy.markUpdated();
        return ReviewPolicyDto.from(policy);
    }

    @Transactional(readOnly = true)
    public RepositoryConnection getRepository(Long repositoryId) {
        return repositoryConnectionRepository.findById(repositoryId)
                .orElseThrow(() -> new EntityNotFoundException("Repository not found"));
    }
}
