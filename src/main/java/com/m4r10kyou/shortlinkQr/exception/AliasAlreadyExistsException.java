package com.m4r10kyou.shortlinkQr.exception;

public class AliasAlreadyExistsException extends RuntimeException {

    private final String alias;

    public AliasAlreadyExistsException(String alias) {
        super(buildMessage(alias));
        this.alias = alias;
    }

    private static String buildMessage(String alias) {
        return "The alias '" + alias + "' is already in use";
    }

    public String getAlias() {
        return alias;
    }
}
