package com.patchlens.api;

import com.patchlens.dto.PlatformCapabilitySummaryDto;
import com.patchlens.service.PlatformCapabilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform-capabilities")
public class PlatformCapabilityController {

    private final PlatformCapabilityService platformCapabilityService;

    public PlatformCapabilityController(PlatformCapabilityService platformCapabilityService) {
        this.platformCapabilityService = platformCapabilityService;
    }

    @GetMapping
    public PlatformCapabilitySummaryDto summary() {
        return platformCapabilityService.summary();
    }
}
