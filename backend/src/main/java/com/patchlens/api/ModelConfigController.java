package com.patchlens.api;

import com.patchlens.dto.ListModelsRequest;
import com.patchlens.dto.ModelConfigDto;
import com.patchlens.dto.ModelConfigTestResult;
import com.patchlens.dto.ModelOptionDto;
import com.patchlens.dto.UpsertModelConfigRequest;
import com.patchlens.service.ModelDiscoveryService;
import com.patchlens.service.AuditLogService;
import com.patchlens.service.ModelConfigService;
import com.patchlens.service.ModelConfigTestService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/model-configs")
public class ModelConfigController {

    private final ModelConfigService modelConfigService;
    private final ModelConfigTestService modelConfigTestService;
    private final ModelDiscoveryService modelDiscoveryService;
    private final AuditLogService auditLogService;

    public ModelConfigController(
            ModelConfigService modelConfigService,
            ModelConfigTestService modelConfigTestService,
            ModelDiscoveryService modelDiscoveryService,
            AuditLogService auditLogService
    ) {
        this.modelConfigService = modelConfigService;
        this.modelConfigTestService = modelConfigTestService;
        this.modelDiscoveryService = modelDiscoveryService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<ModelConfigDto> list() {
        return modelConfigService.list();
    }

    @PostMapping
    public ModelConfigDto create(@Valid @RequestBody UpsertModelConfigRequest request) {
        ModelConfigDto dto = modelConfigService.create(request);
        auditLogService.record("MODEL_CONFIG_CREATE", "MODEL_CONFIG", dto.id(), dto.provider() + "/" + dto.modelName());
        return dto;
    }

    @PostMapping("/models")
    public List<ModelOptionDto> listModels(@Valid @RequestBody ListModelsRequest request) {
        return modelDiscoveryService.listModels(request);
    }

    @GetMapping("/{id}/models")
    public List<ModelOptionDto> listSavedModels(@PathVariable Long id) {
        return modelDiscoveryService.listModels(modelConfigService.getRuntimeConfig(id));
    }

    @PutMapping("/{id}")
    public ModelConfigDto update(@PathVariable Long id, @Valid @RequestBody UpsertModelConfigRequest request) {
        ModelConfigDto dto = modelConfigService.update(id, request);
        auditLogService.record("MODEL_CONFIG_UPDATE", "MODEL_CONFIG", id, dto.provider() + "/" + dto.modelName());
        return dto;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        modelConfigService.delete(id);
        auditLogService.record("MODEL_CONFIG_DELETE", "MODEL_CONFIG", id, "删除模型配置");
    }

    @PostMapping("/{id}/activate")
    public ModelConfigDto activate(@PathVariable Long id) {
        ModelConfigDto dto = modelConfigService.activate(id);
        auditLogService.record("MODEL_CONFIG_ACTIVATE", "MODEL_CONFIG", id, dto.modelName());
        return dto;
    }

    @PostMapping("/{id}/test")
    public ModelConfigTestResult test(@PathVariable Long id) {
        return modelConfigTestService.test(id);
    }
}
