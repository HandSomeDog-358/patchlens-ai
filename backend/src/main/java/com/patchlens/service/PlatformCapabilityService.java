package com.patchlens.service;

import com.patchlens.domain.RepositoryConnection;
import com.patchlens.domain.RepositoryProvider;
import com.patchlens.dto.ModelConfigDto;
import com.patchlens.dto.PlatformCapabilityDto;
import com.patchlens.dto.PlatformCapabilitySummaryDto;
import com.patchlens.dto.PlatformConfigDto;
import com.patchlens.repository.RepositoryConnectionRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformCapabilityService {

    private final PlatformConfigService platformConfigService;
    private final ModelConfigService modelConfigService;
    private final RepositoryConnectionRepository repositoryConnectionRepository;

    public PlatformCapabilityService(
            PlatformConfigService platformConfigService,
            ModelConfigService modelConfigService,
            RepositoryConnectionRepository repositoryConnectionRepository
    ) {
        this.platformConfigService = platformConfigService;
        this.modelConfigService = modelConfigService;
        this.repositoryConnectionRepository = repositoryConnectionRepository;
    }

    @Transactional(readOnly = true)
    public PlatformCapabilitySummaryDto summary() {
        List<PlatformConfigDto> configs = platformConfigService.list();
        Map<String, PlatformConfigDto> configsByProvider = configs.stream()
                .collect(Collectors.toMap(PlatformConfigDto::provider, Function.identity(), (left, right) -> right));
        List<ModelConfigDto> modelConfigs = modelConfigService.list();
        String activeModelName = modelConfigs.stream()
                .filter(ModelConfigDto::enabled)
                .findFirst()
                .map(config -> config.provider() + " / " + config.modelName())
                .orElse("");
        boolean activeModelReady = modelConfigs.stream()
                .anyMatch(config -> config.enabled() && config.hasApiKey());
        List<RepositoryConnection> repositories = repositoryConnectionRepository.findAll();
        List<PlatformCapabilityDto> capabilities = Arrays.stream(RepositoryProvider.values())
                .map(provider -> capability(provider, configsByProvider.get(provider.name()), repositories))
                .toList();

        return new PlatformCapabilitySummaryDto(
                repositories.size(),
                repositories.stream().filter(RepositoryConnection::isEnabled).count(),
                configs.stream().filter(PlatformConfigDto::enabled).count(),
                activeModelReady,
                activeModelName,
                capabilities
        );
    }

    private PlatformCapabilityDto capability(
            RepositoryProvider provider,
            PlatformConfigDto config,
            List<RepositoryConnection> repositories
    ) {
        boolean configurable = provider == RepositoryProvider.GITHUB || provider == RepositoryProvider.GITEE || provider == RepositoryProvider.GITEA;
        boolean configured = config != null;
        boolean enabled = config != null && config.enabled();
        long repositoryCount = repositories.stream()
                .filter(repository -> repository.getProvider() == provider)
                .count();
        long enabledRepositoryCount = repositories.stream()
                .filter(repository -> repository.getProvider() == provider && repository.isEnabled())
                .count();
        List<String> gaps = new ArrayList<>();
        if (!configurable) {
            gaps.add("当前版本尚未提供平台令牌配置入口");
        } else if (!configured) {
            gaps.add("尚未配置平台 API 地址和令牌");
        } else {
            if (!enabled) {
                gaps.add("平台配置未启用");
            }
            if (!config.hasAccessToken()) {
                gaps.add("缺少访问令牌，无法拉取提交或代码差异");
            }
            if (!config.hasWebhookSecret()) {
                gaps.add("缺少 Webhook secret，无法校验平台回调签名");
            }
        }
        if (repositoryCount == 0) {
            gaps.add("尚未接入该平台仓库");
        }

        return new PlatformCapabilityDto(
                provider.name(),
                displayName(provider),
                configurable,
                configured,
                enabled,
                config == null ? "" : config.apiBaseUrl(),
                config != null && config.hasAccessToken(),
                config != null && config.hasWebhookSecret(),
                repositoryCount,
                enabledRepositoryCount,
                configurable,
                configurable,
                configurable,
                configurable,
                configurable,
                true,
                gaps.isEmpty() ? "READY" : "ATTENTION",
                gaps
        );
    }

    private String displayName(RepositoryProvider provider) {
        return switch (provider) {
            case GITEE -> "Gitee";
            case GITEA -> "Gitea";
            case GITHUB -> "GitHub";
            case GITLAB -> "GitLab";
        };
    }
}
