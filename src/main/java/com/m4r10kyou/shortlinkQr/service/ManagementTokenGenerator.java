package com.m4r10kyou.shortlinkQr.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

@Component
public class ManagementTokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();
    private final HexFormat hexFormat = HexFormat.of();
    private final Base64.Encoder encoderBase64 = Base64.getUrlEncoder().withoutPadding();

    public record Token(String rawToken, String tokenHash) {}

    public Token generate(){

        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);

        String rawToken = encoderBase64.encodeToString(tokenBytes);

        return new Token(rawToken,  hash(rawToken));
    }

    public String hash(String rawToken){

        Objects.requireNonNull(rawToken, "Raw token cannot be null");

        try{

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            return hexFormat.formatHex(hashBytes);

        }catch(NoSuchAlgorithmException e) {

            throw new IllegalStateException("Cannot generate hash at this moment", e);
        }
    }
}
