package com.m4r10kyou.shortlinkQr.domain;

import java.util.Set;

public final class ShortLinkConstraints {

    private  ShortLinkConstraints(){

    }

    public static final String CODE_ALLOWED_CHARS = "[a-zA-Z0-9_-]";
    public static final int CODE_MIN_LENGTH = 3;
    public static final int CODE_MAX_LENGTH = 50;

    /*
     * Compared ignoring case, which rejects more than strictly necessary:
     * Spring routes case-sensitively, so only these exact spellings collide.
     * The cost is that nobody can claim "API" as an alias; the gain is that
     * no two aliases differ only in case and behave differently.
     */
    public static final Set<String> NOT_ALLOWED_ALIAS = Set.of("api", "error", "h2-console", "actuator");

}
