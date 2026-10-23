package com.m4r10kyou.shortlinkQr.service;

import com.m4r10kyou.shortlinkQr.exception.InvalidAdminTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class AdminTokenVerifier {


    private final ManagementTokenGenerator managementTokenGenerator;
    private final String adminTokenHash;

    public AdminTokenVerifier(@Value("${shortlink.admin-token}") String token ,
                              ManagementTokenGenerator managementTokenGenerator) {

        this.managementTokenGenerator = managementTokenGenerator;
        this.adminTokenHash = managementTokenGenerator.hash(token);
    }

    // Also closes an unconfigured server: without this branch a blank header
    // would hash equal to a blank configured token and be authorised.
    public boolean verify(String rawToken){

        if(null == rawToken){

            return false;
        } else if( rawToken.isBlank()) {

            throw new InvalidAdminTokenException("Token provided cannot be blank");
        }

        byte[] hashTokenProvidedBytes = managementTokenGenerator.hash(rawToken).getBytes(StandardCharsets.UTF_8);
        byte[] hashTokenAdminBytes    = adminTokenHash.getBytes(StandardCharsets.UTF_8);

        if(!MessageDigest.isEqual(hashTokenProvidedBytes, hashTokenAdminBytes)){

            throw new InvalidAdminTokenException("Token provided for check health is invalid");
        }

        return true;
    }
}
