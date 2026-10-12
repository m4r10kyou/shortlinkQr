package com.m4r10kyou.shortlinkQr.exception;

public class InvalidLogoException extends RuntimeException {

    public InvalidLogoException(String message) {

        super(message);
    }

    public InvalidLogoException(String message,Throwable cause) {

        super(message, cause);
    }
}
