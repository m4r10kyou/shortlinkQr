package com.m4r10kyou.shortlinkQr.exception;

public class ShortLinkDeletedException extends RuntimeException{

    private final String code;

    public ShortLinkDeletedException(String code) {
        super(buildMessage(code));
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    private static String buildMessage(String code) {
        return ("Code '" + code + "' is deleted");
    }
}
