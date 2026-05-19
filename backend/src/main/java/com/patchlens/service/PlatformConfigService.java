package com.patchlens.service;

import com.patchlens.config.PatchLensProperties;
import com.patchlens.domain.PlatformConfig;
import com.patchlens.domain.RepositoryProvider;
import com.patchlens.dto.PlatformConfigDto;
import com.patchlens.dto.UpsertPlatformConfigRequest;
import com.patchlens.repository.PlatformConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PlatformConfigService {

    private final PlatformConfigRepository platformConfigRepository;
    private final PatchLensProperties properties;
    private final SecretCryptoService secretCryptoService;

    public PlatformConfigService(
            PlatformConfigRepository platformConfigRepository,
            PatchLensProperties properties,
            SecretCryptoService secretCryptoService
    ) {
        this.platformConfigRepository = platformConfigRepository;
        this.properties = properties;
        this.secretCryptoService = secretCryptoService;
    }

    @Transactional(readOnly = true)
    public List<PlatformConfigDto> list() {
        return platformConfigRepository.findAll().stream()
                .map(PlatformConfigDto::from)
                .toList();
    }

    @Transactional
    public PlatformConfigDto create(UpsertPlatformConfigRequest request) {
        RepositoryProvider provider = parseProvider(request.provider());
        if (platformConfigRepository.existsByProvider(provider)) {
            throw new IllegalArgumentException("Platform config already exists for " + provider);
        }
        PlatformConfig config = new PlatformConfig();
        apply(config, provider, request);
        return PlatformConfigDto.from(platformConfigRepository.save(config));
    }

    @Transactional
    public PlatformConfigDto update(Long id, UpsertPlatformConfigRequest request) {
        PlatformConfig config = platformConfigRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Platform config not found"));
        apply(config, parseProvider(request.provider()), request);
        config.markUpdated();
        return PlatformConfigDto.from(config);
    }

    @Transactional
    public void delete(Long id) {
        platformConfigRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ResolvedPlatformConfig> resolve(RepositoryProvider provider) {
        Optional<PlatformConfig> saved = platformConfigRepository
                .findFirstByProviderAndEnabledTrueOrderByUpdatedAtDesc(provider);
        if (saved.isPresent()) {
            PlatformConfig config = saved.get();
            return Optional.of(new ResolvedPlatformConfig(
                    config.getApiBaseUrl(),
                    decode(config.getAccessTokenEncrypted()),
                    decode(config.getWebhookSecretEncrypted())
            ));
        }
        return resolveFromProperties(provider);
    }

    private Optional<ResolvedPlatformConfig> resolveFromProperties(RepositoryProvider provider) {
        if (provider == RepositoryProvider.GITHUB && properties.github() != null) {
            return Optional.of(new ResolvedPlatformConfig(
                    properties.github().apiBaseUrl(),
                    properties.github().accessToken(),
                    properties.github().webhookSecret()
            ));
        }
        if (provider == RepositoryProvider.GITEE && properties.gitee() != null) {
            return Optional.of(new ResolvedPlatformConfig(
                    properties.gitee().apiBaseUrl(),
                    properties.gitee().accessToken(),
                    properties.gitee().webhookSecret()
            ));
        }
        if (provider == RepositoryProvider.GITEA && properties.gitea() != null) {
            return Optional.of(new ResolvedPlatformConfig(
                    properties.gitea().apiBaseUrl(),
                    properties.gitea().accessToken(),
                    properties.gitea().webhookSecret()
            ));
        }
        return Optional.empty();
    }

    private void apply(PlatformConfig config, RepositoryProvider provider, UpsertPlatformConfigRequest request) {
        config.setProvider(provider);
        config.setApiBaseUrl(request.apiBaseUrl());
        config.setEnabled(request.enabled());
        if (StringUtils.hasText(request.accessToken())) {
            config.setAccessTokenEncrypted(encode(request.accessToken()));
        }
        if (StringUtils.hasText(request.webhookSecret())) {
            config.setWebhookSecretEncrypted(encode(request.webhookSecret()));
        }
    }

    private RepositoryProvider parseProvider(String value) {
        RepositoryProvider provider = RepositoryProvider.valueOf(value.toUpperCase());
        if (provider != RepositoryProvider.GITHUB && provider != RepositoryProvider.GITEE && provider != RepositoryProvider.GITEA) {
            throw new IllegalArgumentException("Only GITHUB, GITEE and GITEA platform configs are supported now");
        }
        return provider;
    }

    private String encode(String value) {
        return secretCryptoService.encrypt(value);
    }

    private String decode(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return secretCryptoService.decrypt(value);
    }

    public record ResolvedPlatformConfig(
            String apiBaseUrl,
            String accessToken,
            String webhookSecret
    ) {
        public boolean hasApiAccess() {
            return StringUtils.hasText(apiBaseUrl) && StringUtils.hasText(accessToken);
        }
    }
}
