package com.patchlens.dto;

import java.util.List;

public record PlatformCapabilitySummaryDto(
        long repositoryCount,
        long enabledRepositoryCount,
        long configuredPlatformCount,
        boolean activeModelReady,
        String activeModelName,
        List<PlatformCapabilityDto> capabilities
) {
}
