package com.m4r10kyou.shortlinkQr.exception;

public class InvalidTokenException extends RuntimeException {

    private final String code;

    public InvalidTokenException(String message, String code) {

        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
