package com.m4r10kyou.shortlinkQr.domain;

import java.time.Instant;

public final class ExpirationPolicy {

    private ExpirationPolicy(){
        //Prevent instantiation
    }

    public static boolean isExpired(Instant expiresAt){

        return  expiresAt != null && expiresAt.isBefore(Instant.now());
    }
}
