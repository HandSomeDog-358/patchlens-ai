package com.patchlens.service;

import com.patchlens.domain.AuditLog;
import com.patchlens.dto.AuditLogDto;
import com.patchlens.dto.PageResponse;
import com.patchlens.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDto> list(int page, int size) {
        return PageResponse.from(auditLogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)))
        ).map(AuditLogDto::from));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String resourceType, Object resourceId, String detail) {
        AuditLog log = new AuditLog();
        log.setActor(currentActor());
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId == null ? "" : String.valueOf(resourceId));
        log.setDetail(StringUtils.hasText(detail) ? detail : "");
        auditLogRepository.save(log);
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "system";
        }
        return authentication.getName();
    }
}
