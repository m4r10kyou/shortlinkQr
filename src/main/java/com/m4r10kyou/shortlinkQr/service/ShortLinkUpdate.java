package com.m4r10kyou.shortlinkQr.service;

import java.time.Instant;

public record ShortLinkUpdate (String targetUrl, Instant expiresAt, boolean clearExpiry){}
