package com.m4r10kyou.shortlinkQr.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.m4r10kyou.shortlinkQr.validation.HttpUrl;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ShortLinkEditionRequest(

    @HttpUrl
    @Size(max = 2048, message = "Max size allowed of URL is 2048")
    String targetUrl,

    @Future(message = "Expiry date must be in the future")
    Instant expiresAt,

    Boolean clearExpiry

    ){

    public ShortLinkEditionRequest {

        if (null != targetUrl) {
            targetUrl = targetUrl.trim();
        }

        if (null == clearExpiry) {
            clearExpiry = false;
        }
    }

    @JsonIgnore
    @AssertTrue(message = "Can not edit Short Link without parameters")
    public boolean isNotEmptyRequest() {
        return targetUrl != null || expiresAt != null || clearExpiry;
    }

    @JsonIgnore
    @AssertTrue(message = "Can not clear expiration giving a date")
    public boolean isNotContradictory() {
        return !(expiresAt != null && clearExpiry);
    }
}
