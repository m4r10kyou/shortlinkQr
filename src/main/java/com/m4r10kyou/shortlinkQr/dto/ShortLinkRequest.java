package com.m4r10kyou.shortlinkQr.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;

public record ShortLinkRequest(

        @NotBlank(message = "Target URL is required")
        @Pattern(
                regexp = "^https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*$",
                message = "Target URL must start with http:// or https:// and use a valid domain"
        )
        @Size(max = 2048, message = "Max size allowed of URL is 2048")
        String targetUrl,

        @Pattern(
                regexp = "^[a-zA-Z0-9_-]+$",
                message = "Alias can only contain letters, numbers, hyphens, and underscores"
        )
        @Size(min = 3, max = 50, message = "Alias must be between 3 and 50 characters")
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
