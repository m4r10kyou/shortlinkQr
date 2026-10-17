package com.m4r10kyou.shortlinkQr.service;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;

public record CreatedShortLink(ShortLink shortLink, String rawToken){}
