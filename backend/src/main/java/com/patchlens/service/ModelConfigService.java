package com.patchlens.service;

import com.patchlens.domain.ModelConfig;
import com.patchlens.dto.ModelConfigDto;
import com.patchlens.dto.UpsertModelConfigRequest;
import com.patchlens.repository.ModelConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ModelConfigService {

    private final ModelConfigRepository modelConfigRepository;
    private final SecretCryptoService secretCryptoService;

    public ModelConfigService(ModelConfigRepository modelConfigRepository, SecretCryptoService secretCryptoService) {
        this.modelConfigRepository = modelConfigRepository;
        this.secretCryptoService = secretCryptoService;
    }

    @Transactional(readOnly = true)
    public List<ModelConfigDto> list() {
        return modelConfigRepository.findAll().stream()
                .map(ModelConfigDto::from)
                .toList();
    }

    @Transactional
    public ModelConfigDto create(UpsertModelConfigRequest request) {
        ModelConfig config = new ModelConfig();
        apply(config, request);
        ModelConfig saved = modelConfigRepository.save(config);
        if (saved.isEnabled()) {
            deactivateOthers(saved.getId());
        }
        return ModelConfigDto.from(saved);
    }

    @Transactional
    public ModelConfigDto update(Long id, UpsertModelConfigRequest request) {
        ModelConfig config = modelConfigRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Model config not found"));
        apply(config, request);
        config.markUpdated();
        if (config.isEnabled()) {
            deactivateOthers(config.getId());
        }
        return ModelConfigDto.from(config);
    }

    @Transactional
    public void delete(Long id) {
        modelConfigRepository.deleteById(id);
    }

    @Transactional
    public ModelConfigDto activate(Long id) {
        ModelConfig config = modelConfigRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Model config not found"));
        config.setEnabled(true);
        config.markUpdated();
        deactivateOthers(config.getId());
        return ModelConfigDto.from(config);
    }

    @Transactional(readOnly = true)
    public RuntimeModelConfig getEnabledRuntimeConfig() {
        ModelConfig config = modelConfigRepository.findFirstByEnabledTrueOrderByUpdatedAtDescIdDesc()
                .orElseThrow(() -> new IllegalStateException("请先在模型配置中启用一个模型"));
        return toRuntimeConfig(config);
    }

    @Transactional(readOnly = true)
    public RuntimeModelConfig getRuntimeConfig(Long id) {
        ModelConfig config = modelConfigRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Model config not found"));
        return toRuntimeConfig(config);
    }

    private RuntimeModelConfig toRuntimeConfig(ModelConfig config) {
        String apiKey = decode(config.getApiKeyEncrypted());
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("模型配置缺少 API Key");
        }
        return new RuntimeModelConfig(
                config.getProvider(),
                config.getBaseUrl(),
                config.getModelName(),
                apiKey
        );
    }

    private void apply(ModelConfig config, UpsertModelConfigRequest request) {
        config.setProvider(request.provider());
        config.setBaseUrl(request.baseUrl());
        config.setModelName(request.modelName());
        config.setEnabled(request.enabled());
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            config.setApiKeyEncrypted(secretCryptoService.encrypt(request.apiKey()));
        }
    }

    private void deactivateOthers(Long activeId) {
        for (ModelConfig other : modelConfigRepository.findByIdNotAndEnabledTrue(activeId)) {
            other.setEnabled(false);
            other.markUpdated();
        }
    }

    private String decode(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return secretCryptoService.decrypt(value);
    }

    public record RuntimeModelConfig(
            String provider,
            String baseUrl,
            String modelName,
            String apiKey
    ) {
    }
}
