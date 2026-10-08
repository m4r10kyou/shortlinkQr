package com.m4r10kyou.shortlinkQr.repository;

import java.time.Instant;

public interface RedirectTarget {

    String getTargetUrl();

    Instant getExpiresAt();
}
