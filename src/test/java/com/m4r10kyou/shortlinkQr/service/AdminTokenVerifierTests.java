package com.m4r10kyou.shortlinkQr.service;

import com.m4r10kyou.shortlinkQr.exception.InvalidAdminTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AdminTokenVerifierTests {

    private static final String VALID_TOKEN = "V4l1DT0k3n";
    private static final String ERROR_MSG ="Token provided for check health is invalid";


    private AdminTokenVerifier adminTokenVerifier;

    @BeforeEach
    void setUp() {
        adminTokenVerifier = new AdminTokenVerifier(VALID_TOKEN, new ManagementTokenGenerator());
    }

    @Test
    void verify_withTheConfiguredToken_returnsTrue(){

        assertThat(adminTokenVerifier.verify(VALID_TOKEN)).isTrue();
    }

    @Test
    void verify_withAWrongToken_throwsInvalidAdminTokenException(){

        String token ="Wr0n6T0k3n";

        assertThatThrownBy(() -> adminTokenVerifier.verify(token))
                .isInstanceOf(InvalidAdminTokenException.class)
                .hasMessage(ERROR_MSG);
    }

    @Test
    void verify_withoutToken_returnsFalse(){

        String token = null;

        assertThat(adminTokenVerifier.verify(token)).isFalse();
    }

    @Test
    void verify_withABlankToken_throwsInvalidAdminTokenException(){

        String token = "";
        String errorMsg = "Token provided cannot be blank";

        assertThatThrownBy(() -> adminTokenVerifier.verify(token))
                .isInstanceOf(InvalidAdminTokenException.class)
                .hasMessage(errorMsg);
    }

    @Test
    void verify_whenNoTokenIsConfigured_throwsInvalidAdminTokenException(){
        String token ="what-ever";

        var verifier = new AdminTokenVerifier("", new ManagementTokenGenerator());

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidAdminTokenException.class)
                .hasMessage(ERROR_MSG);
    }

    @Test
    void verify_whenNoTokenIsConfiguredAndBlankProvided_throwsInvalidAdminTokenException(){
        String token ="";
        String errorMsg = "Token provided cannot be blank";

        var verifier = new AdminTokenVerifier("", new ManagementTokenGenerator());

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidAdminTokenException.class)
                .hasMessage(errorMsg);
    }
}
