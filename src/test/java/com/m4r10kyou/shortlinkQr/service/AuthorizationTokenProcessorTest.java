package com.m4r10kyou.shortlinkQr.service;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.exception.InvalidTokenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationTokenProcessorTest {

    @Mock
    private ManagementTokenGenerator tokenGenerator;

    @InjectMocks
    private AuthorizationTokenProcessor authorizationTokenProcessor;

    private final String hashToken = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";
    private final String anotherHash = "b1b2c3d4e5f6b1b2c3d4e5f6b1b2c3d4e5f6b1b2c3d4e5f6b1b2c3d4e5f6b1b2";
    private final ShortLink shortLink = new ShortLink("mi-repo", "https://example.com",null,hashToken);

    @Test
    void authorize_withTheCorrectToken_doesNotThrow(){

        String rawToken = "my-valid-token";

        when(tokenGenerator.hash(rawToken)).thenReturn(hashToken);

        assertThatCode(() -> authorizationTokenProcessor.authorize(shortLink, rawToken))
                .doesNotThrowAnyException();
    }

    @Test
    void authorize_withAnIncorrectToken_throwsInvalidTokenException(){

        String incorrectToken = "incorrectToken-token";
        String errorMsg = "Token provided is invalid";

        when(tokenGenerator.hash(incorrectToken))
                .thenReturn(anotherHash);

        assertThatThrownBy(() -> authorizationTokenProcessor.authorize(shortLink, incorrectToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage(errorMsg);
    }

    @Test
    void authorize_withNull_throwsInvalidTokenException(){

        String errorMsg = "Management token is missing";

        assertThatThrownBy(() -> authorizationTokenProcessor.authorize(shortLink, null))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage(errorMsg);
    }

    @Test
    void authorize_withBlank_throwsInvalidTokenException(){

        String errorMsg = "Management token is missing";

        assertThatThrownBy(() -> authorizationTokenProcessor.authorize(shortLink, "   "))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage(errorMsg);
    }

    @Test
    void authorize_whenTheLinkHasNoStoredHash_throwsInvalidTokenException(){

        ShortLink shortLinkNoHash = new ShortLink("mi-repo-without-hash", "https://example.com",null,null);
        String errorMsg = "Token provided is invalid";
        String rawToken = "some-token";

        when(tokenGenerator.hash(rawToken)).thenReturn(hashToken);

        assertThatThrownBy(() -> authorizationTokenProcessor.authorize(shortLinkNoHash, rawToken ))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage(errorMsg);

    }
}
