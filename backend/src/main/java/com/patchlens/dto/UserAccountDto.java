package com.patchlens.dto;

import com.patchlens.domain.UserAccount;
import java.time.Instant;

public record UserAccountDto(
        Long id,
        String username,
        String displayName,
        String role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserAccountDto from(UserAccount account) {
        return new UserAccountDto(
                account.getId(),
                account.getUsername(),
                account.getDisplayName(),
                account.getRole(),
                account.isEnabled(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
