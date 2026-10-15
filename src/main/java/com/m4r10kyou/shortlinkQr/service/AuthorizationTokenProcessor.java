package com.m4r10kyou.shortlinkQr.service;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.exception.InvalidTokenException;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationTokenProcessor {

    private final ManagementTokenGenerator managementTokenGenerator;

    public AuthorizationTokenProcessor(ManagementTokenGenerator managementTokenGenerator) {
        this.managementTokenGenerator = managementTokenGenerator;
    }

    public void authorize(ShortLink shortLink, String rawToken){

        String hashToken;
        boolean matchToken;

        if(null == rawToken || rawToken.isBlank() ){

            throw new InvalidTokenException("Management token is missing", shortLink.getCode());
        }

        hashToken = managementTokenGenerator.hash(rawToken);

        matchToken = shortLink.matchesManagementToken(hashToken);

        if(!matchToken){

            throw new InvalidTokenException("Token provided is invalid", shortLink.getCode());
        }
    }
}
