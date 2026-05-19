package com.patchlens.dto;

import java.util.List;

public record ReviewPreflightResponse(
        boolean ready,
        List<ReviewPreflightCheckDto> checks
) {
}
