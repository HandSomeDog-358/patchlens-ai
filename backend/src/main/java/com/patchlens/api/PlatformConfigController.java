package com.patchlens.api;

import com.patchlens.dto.PlatformConfigDto;
import com.patchlens.dto.UpsertPlatformConfigRequest;
import com.patchlens.service.AuditLogService;
import com.patchlens.service.PlatformConfigService;
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
@RequestMapping("/api/platform-configs")
public class PlatformConfigController {

    private final PlatformConfigService platformConfigService;
    private final AuditLogService auditLogService;

    public PlatformConfigController(PlatformConfigService platformConfigService, AuditLogService auditLogService) {
        this.platformConfigService = platformConfigService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<PlatformConfigDto> list() {
        return platformConfigService.list();
    }

    @PostMapping
    public PlatformConfigDto create(@Valid @RequestBody UpsertPlatformConfigRequest request) {
        PlatformConfigDto dto = platformConfigService.create(request);
        auditLogService.record("PLATFORM_CONFIG_CREATE", "PLATFORM_CONFIG", dto.id(), dto.provider());
        return dto;
    }

    @PutMapping("/{id}")
    public PlatformConfigDto update(
            @PathVariable Long id,
            @Valid @RequestBody UpsertPlatformConfigRequest request
    ) {
        PlatformConfigDto dto = platformConfigService.update(id, request);
        auditLogService.record("PLATFORM_CONFIG_UPDATE", "PLATFORM_CONFIG", id, dto.provider());
        return dto;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        platformConfigService.delete(id);
        auditLogService.record("PLATFORM_CONFIG_DELETE", "PLATFORM_CONFIG", id, "删除平台配置");
    }
}
