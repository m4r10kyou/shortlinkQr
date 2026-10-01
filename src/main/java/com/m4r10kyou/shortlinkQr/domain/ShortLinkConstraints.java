package com.m4r10kyou.shortlinkQr.domain;

public final class ShortLinkConstraints {

    private  ShortLinkConstraints(){

    }

    public static final String CODE_ALLOWED_CHARS = "[a-zA-Z0-9_-]";
    public static final int CODE_MIN_LENGTH = 3;
    public static final int CODE_MAX_LENGTH = 50;
}
