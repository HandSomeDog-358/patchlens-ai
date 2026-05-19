package com.patchlens.service;

import com.patchlens.domain.RepositoryProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WebhookSecurityService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final PlatformConfigService platformConfigService;

    public WebhookSecurityService(PlatformConfigService platformConfigService) {
        this.platformConfigService = platformConfigService;
    }

    public void validate(RepositoryProvider provider, Map<String, String> headers, String payload) {
        String secret = platformConfigService.resolve(provider)
                .map(PlatformConfigService.ResolvedPlatformConfig::webhookSecret)
                .filter(StringUtils::hasText)
                .orElse("");
        if (!StringUtils.hasText(secret) || "dev-secret".equals(secret)) {
            return;
        }

        if (matchesPlainToken(headers, secret) || matchesHmacSignature(headers, payload, secret)) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook secret");
    }

    private boolean matchesPlainToken(Map<String, String> headers, String secret) {
        for (String headerName : new String[] {
                "x-gitee-token",
                "x-gitee-password",
                "x-gitea-token",
                "x-hub-token",
                "x-webhook-token",
                "x-webhook-secret"
        }) {
            String value = header(headers, headerName);
            if (StringUtils.hasText(value) && constantTimeEquals(value.trim(), secret)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesHmacSignature(Map<String, String> headers, String payload, String secret) {
        String expected = hmacSha256(payload, secret);
        for (String headerName : new String[] {
                "x-gitee-signature",
                "x-gitea-signature",
                "x-hub-signature-256"
        }) {
            String actual = normalizeSignature(header(headers, headerName));
            if (StringUtils.hasText(actual) && constantTimeEquals(actual, expected)) {
                return true;
            }
        }
        return false;
    }

    private String header(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return "";
    }

    private String normalizeSignature(String signature) {
        if (!StringUtils.hasText(signature)) {
            return "";
        }
        String trimmed = signature.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("sha256=")) {
            return trimmed.substring("sha256=".length());
        }
        return trimmed;
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate webhook signature", ex);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
