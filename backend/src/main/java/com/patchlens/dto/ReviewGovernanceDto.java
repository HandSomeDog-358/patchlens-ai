package com.patchlens.dto;

public record ReviewGovernanceDto(
        long queued,
        long running,
        long completed,
        long failed,
        long canceled,
        long stuck,
        int stuckMinutes
) {
}
