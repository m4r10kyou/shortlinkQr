package com.m4r10kyou.shortlinkQr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class CodeGenerator {

    private static final String ALPHABET = "aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ0123456789";
    private static final int BASE = ALPHABET.length();
    private static final int CODE_LENGTH = 7;
    private final int numRetries;

    private final SecureRandom random = new SecureRandom();

    public CodeGenerator(@Value("${shortlink.generator.max-retries:5}") int maxRetries) {
        this.numRetries = maxRetries;
    }

    public String generate(Predicate<String> isUsed){

        for(int retry = 0; retry < numRetries ; retry ++){

            String code = generateRandomCode();

            if (!isUsed.test(code)) {
                return code;
            }
        }

        throw new IllegalStateException("Cannot generate code after " + numRetries + " attempts.");
    }

    private String generateRandomCode() {

        return random.ints(CODE_LENGTH, 0, BASE)
                .mapToObj(index -> String.valueOf(ALPHABET.charAt(index)))
                .collect(Collectors.joining());
    }
}
