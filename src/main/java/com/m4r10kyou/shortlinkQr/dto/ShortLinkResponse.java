package com.m4r10kyou.shortlinkQr.dto;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;

import java.time.Instant;

public record ShortLinkResponse (
        String shortUrl,
        String targetUrl,
        Instant createdAt,
        Instant expiresAt,
        int visitCount,
        boolean hasLogo
) {

    public static ShortLinkResponse from(ShortLink shortLink, String normalisedBaseUrl) {

        String finalUrl = normalisedBaseUrl + shortLink.getCode();

        return new ShortLinkResponse(
                finalUrl,
                shortLink.getTargetUrl(),
                shortLink.getCreatedAt(),
                shortLink.getExpiresAt(),
                shortLink.getVisitCount(),
                shortLink.hasLogo()
        );
    }
}
