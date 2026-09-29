package com.m4r10kyou.shortlinkQr.dto;

import com.m4r10kyou.shortlinkQr.domain.ShortLinkConstraints;
import com.m4r10kyou.shortlinkQr.validation.HttpUrl;
import jakarta.validation.constraints.*;

import java.time.Instant;

public record ShortLinkRequest(

        @NotBlank(message = "Target URL is required")
        @HttpUrl
        @Size(max = 2048, message = "Max size allowed of URL is 2048")
        String targetUrl,

        @Pattern(
                regexp = ShortLinkConstraints.CODE_ALLOWED_CHARS+"+",
                message = "Alias can only contain letters, numbers, hyphens, and underscores"
        )
        @Size(
                min = ShortLinkConstraints.CODE_MIN_LENGTH,
                max = ShortLinkConstraints.CODE_MAX_LENGTH,
                message = "Alias must be between {min} and {max} characters")
        String customAlias,

        @Future(message = "Expiry date must be in the future")
        Instant expiresAt

) {

    public ShortLinkRequest {

        if (targetUrl != null) {
            targetUrl = targetUrl.trim();
        }

        if (customAlias != null) {
            if (customAlias.isBlank()) {
                customAlias = null;
            } else {
                customAlias = customAlias.trim();
            }
        }
    }
}
