package com.m4r10kyou.shortlinkQr.exception;

public class AliasAlreadyExistsException extends RuntimeException{

    public AliasAlreadyExistsException(String message) {
        super(message);
    }
}
