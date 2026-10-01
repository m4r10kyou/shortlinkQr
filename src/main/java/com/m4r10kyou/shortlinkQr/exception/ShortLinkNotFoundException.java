package com.m4r10kyou.shortlinkQr.exception;

public class ShortLinkNotFoundException extends RuntimeException {

    private final String code;

    public ShortLinkNotFoundException(String code) {
        super(buildMessage(code));
        this.code = code;
    }

    private static String buildMessage(String code) {
        return ("Code '" + code + "' does not exist!");
    }

    public String getCode() {
        return code;
    }
}
