package com.m4r10kyou.shortlinkQr.dto;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;

import java.time.Instant;

public record ShortLinkEditionResponse(
        String shortUrl,
        String targetUrl,
        Instant createdAt,
        Instant expiresAt,
        boolean hasLogo
) {

    public static ShortLinkEditionResponse from(ShortLink shortLink, String normalisedBaseUrl) {

        String finalUrl = normalisedBaseUrl + shortLink.getCode();

        return new ShortLinkEditionResponse(
                finalUrl,
                shortLink.getTargetUrl(),
                shortLink.getCreatedAt(),
                shortLink.getExpiresAt(),
                shortLink.hasLogo()
        );
    }
}
