package com.m4r10kyou.shortlinkQr.dto;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;

import java.time.Instant;

public record ShortLinkCreationResponse(
        String shortUrl,
        String targetUrl,
        Instant createdAt,
        Instant expiresAt,
        int visitCount,
        boolean hasLogo,
        String managementToken
) {

    public static ShortLinkCreationResponse from(ShortLink shortLink, String normalisedBaseUrl, String rawToken) {

        String finalUrl = normalisedBaseUrl + shortLink.getCode();

        return new ShortLinkCreationResponse(
                finalUrl,
                shortLink.getTargetUrl(),
                shortLink.getCreatedAt(),
                shortLink.getExpiresAt(),
                shortLink.getVisitCount(),
                shortLink.hasLogo(),
                rawToken
        );
    }
}
