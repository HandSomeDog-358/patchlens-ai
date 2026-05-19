package com.patchlens.dto;

import com.patchlens.domain.AuditLog;
import java.time.Instant;

public record AuditLogDto(
        Long id,
        String actor,
        String action,
        String resourceType,
        String resourceId,
        String detail,
        Instant createdAt
) {
    public static AuditLogDto from(AuditLog log) {
        return new AuditLogDto(
                log.getId(),
                log.getActor(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getDetail(),
                log.getCreatedAt()
        );
    }
}
