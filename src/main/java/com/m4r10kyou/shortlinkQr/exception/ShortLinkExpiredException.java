package com.m4r10kyou.shortlinkQr.exception;

public class ShortLinkExpiredException extends RuntimeException{

    public ShortLinkExpiredException(String message) {
        super(message);
    }
}
