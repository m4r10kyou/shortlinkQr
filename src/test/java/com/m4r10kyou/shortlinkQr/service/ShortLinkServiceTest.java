package com.m4r10kyou.shortlinkQr.service;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.exception.AliasAlreadyExistsException;
import com.m4r10kyou.shortlinkQr.exception.InvalidTokenException;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkExpiredException;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkNotFoundException;
import com.m4r10kyou.shortlinkQr.repository.RedirectTarget;
import com.m4r10kyou.shortlinkQr.repository.ShortLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceTest {

    private final String hashToken = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private CodeGenerator codeGenerator;

    @Mock
    private ManagementTokenGenerator managementTokenGenerator;

    @Mock
    private AuthorizationTokenProcessor authorizationTokenProcessor;

    @Captor
    private ArgumentCaptor<ShortLink> shortLinkCaptor;

    @InjectMocks
    private ShortLinkService shortLinkService;

    @Test
    void createLink_withFreeAlias_usesAliasAndDoesNotCallGenerator() {

        String target = "https://example.com";
        String alias = "my-alias";

        when(shortLinkRepository.existsByCode(alias)).thenReturn(false);
        stubTokenGenerator();
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortLink result = shortLinkService.createLink(target, alias, null).shortLink();

        assertThat(result.getCode()).isEqualTo(alias);
        assertThat(result.getTargetUrl()).isEqualTo(target);

        verify(codeGenerator, never()).generate(any());
        verify(shortLinkRepository).save(any(ShortLink.class));
    }

    @Test
    void createLink_withTakenAlias_throwsAliasAlreadyExistsException() {

        String target = "https://example.com";
        String alias = "alias-used";

        when(shortLinkRepository.existsByCode(alias)).thenReturn(true);

        assertThatThrownBy(() -> shortLinkService.createLink(target, alias, null))
                .isInstanceOf(AliasAlreadyExistsException.class)
                .hasMessageContaining(alias);

        verify(shortLinkRepository, never()).save(any());
    }

    @Test
    void createLink_withoutAlias_usesGenerator() {

        String target = "https://example.com";
        String generatedCode = "gen123";

        when(codeGenerator.generate(any())).thenReturn(generatedCode);
        stubTokenGenerator();
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortLink result = shortLinkService.createLink(target, null, null).shortLink();

        assertThat(result.getCode()).isEqualTo(generatedCode);

        verify(codeGenerator).generate(any());
        verify(shortLinkRepository).save(any(ShortLink.class));
    }

    @Test
    void resolveCode_withValidCode_returnsTargetUrlAndCountsVisit() {

        String target = "https://example.com";
        String code = "valid";

        RedirectTarget redirectTarget = redirectTargetExpiringAt(target, null);

        when(shortLinkRepository.findTargetByCode(code)).thenReturn(Optional.of(redirectTarget));

        String result = shortLinkService.resolveCode(code);

        assertThat(result).isEqualTo(target);

        verify(shortLinkRepository).registerVisitCount(code);
    }

    @Test
    void resolveCode_withNotFoundCode_throwsShortLinkNotFoundException() {

        String code = "not-exist";

        when(shortLinkRepository.findTargetByCode(code)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shortLinkService.resolveCode(code))
                .isInstanceOf(ShortLinkNotFoundException.class)
                .hasMessageContaining(code);

        verify(shortLinkRepository, never()).registerVisitCount(any());
    }

    @Test
    void resolveCode_withExpiredLink_throwsShortLinkExpiredException() {

        String target = "https://example.com";
        String code = "expired";
        Instant pastExpiration = Instant.now().minus(1, ChronoUnit.HOURS);
        RedirectTarget redirectTarget = redirectTargetExpiringAt(target, pastExpiration);

        when(shortLinkRepository.findTargetByCode(code)).thenReturn(Optional.of(redirectTarget));

        assertThatThrownBy(() -> shortLinkService.resolveCode(code))
                .isInstanceOf(ShortLinkExpiredException.class)
                .hasMessageContaining(code);

        verify(shortLinkRepository, never()).registerVisitCount(any());
    }

    @Test
    void updateLink_withNotFoundCode_throwsShortLinkNotFoundException() {

        String code = "not-exist-code-target-url";
        String target = "https://false.example.com";
        String rawToken = "someToken";

        when(shortLinkRepository.findByCode(code)).thenReturn(Optional.empty());

        ShortLinkUpdate slUpdate = new ShortLinkUpdate(target, Instant.now().plus(5, ChronoUnit.DAYS), false);

        assertThatThrownBy(() -> shortLinkService.updateLink(code, slUpdate, rawToken))
                .isInstanceOf(ShortLinkNotFoundException.class)
                .hasMessageContaining(code);
    }

    @Test
    void attachLogo_withNotFoundCode_throwsShortLinkNotFoundException() {

        String code = "not-exist-code-attach-logo";
        String token = "validToken";

        byte[] pngSignature = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        when(shortLinkRepository.findByCode(code)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shortLinkService.attachLogo(code, pngSignature, token))
                .isInstanceOf(ShortLinkNotFoundException.class)
                .hasMessageContaining(code);
    }

    @Test
    void attachLogo_withValidCode_storesTheLogo() {

        String code = "my-repo";
        String urlTarget = "https://new.example.com";
        String token = "validToken";

        ShortLink link = new ShortLink(code, urlTarget, null, hashToken);

        byte[] pngSignature = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        when(shortLinkRepository.findByCode(code)).thenReturn(Optional.of(link));

        ShortLink result = shortLinkService.attachLogo(code, pngSignature, token);

        assertThat(result.hasLogo()).isTrue();
        assertThat(result.getLogoData()).isEqualTo(pngSignature);
    }

    @Test
    void attachLogo_withAnInvalidToken_doesNotAttachTheLogo() {

        String code = "my-repo";
        String urlTarget = "https://new.example.com";
        String tokenInvalid = "noValidToken";
        String errorMsg = "Token provided is invalid";

        ShortLink link = new ShortLink(code, urlTarget, null, hashToken);

        byte[] pngSignature = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        when(shortLinkRepository.findByCode(code)).thenReturn(Optional.of(link));

        doThrow(new InvalidTokenException(errorMsg, code))
                .when(authorizationTokenProcessor).authorize(link, tokenInvalid);

        assertThatThrownBy(() -> shortLinkService.attachLogo(code, pngSignature, tokenInvalid))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining(errorMsg);

        assertThat(link.hasLogo()).isFalse();
    }

    @Test
    void createLink_withACustomAlias_storesTheGeneratedTokenHash() {

        String target = "https://example.com";
        String alias = "custom-alias-token";

        when(shortLinkRepository.existsByCode(alias)).thenReturn(false);
        stubTokenGenerator();

        shortLinkService.createLink(target, alias, null);

        verify(shortLinkRepository).save(shortLinkCaptor.capture());

        ShortLink capturedLink = shortLinkCaptor.getValue();
        assertThat(capturedLink.getTargetUrl()).isEqualTo(target);
        assertThat(capturedLink.getCode()).isEqualTo(alias);
        assertThat(capturedLink.matchesManagementToken(hashToken)).isTrue();
    }

    @Test
    void createLink_withoutAnAlias_storesTheGeneratedTokenHash() {

        String target = "https://example.com";
        String generatedCode = "randomAliasGenerated";

        when(codeGenerator.generate(any())).thenReturn(generatedCode);
        stubTokenGenerator();

        shortLinkService.createLink(target, null, null);

        verify(shortLinkRepository).save(shortLinkCaptor.capture());

        ShortLink capturedLink = shortLinkCaptor.getValue();
        assertThat(capturedLink.getTargetUrl()).isEqualTo(target);
        assertThat(capturedLink.getCode()).isEqualTo(generatedCode);
        assertThat(capturedLink.matchesManagementToken(hashToken)).isTrue();
    }

    @Test
    void updateLink_withANewTargetUrl_changesOnlyTheTargetUrl() {

        String code = "newTargetUrl";
        String oldTarget = "https://example.com";
        String newTarget = "https://new.example.com";
        Instant originalExpiry = Instant.now().plus(10, ChronoUnit.DAYS);
        String token = "validToken";

        ShortLink link = new ShortLink(code, oldTarget, originalExpiry, hashToken);

        when(shortLinkRepository.findByCode(code)).thenReturn(Optional.of(link));

        ShortLinkUpdate slUpdate = new ShortLinkUpdate(newTarget, null, false);

        ShortLink result = shortLinkService.updateLink(code, slUpdate, token);

        assertThat(result.getTargetUrl()).isEqualTo(newTarget);
        assertThat(result.getExpiresAt()).isEqualTo(originalExpiry);
    }

    @Test
    void updateLink_withANewExpiry_changesOnlyTheExpiry() {

        String code = "newExpiry";
        Instant newInstant = Instant.now().plus(25, ChronoUnit.DAYS);
        String originalTargetUrl = "https://example.com";
        String token = "validToken";

        ShortLink link = new ShortLink(code, originalTargetUrl, null, hashToken);

        when(shortLinkRepository.findByCode(code)).thenReturn(Optional.of(link));

        ShortLinkUpdate slUpdate = new ShortLinkUpdate(null, newInstant, false);

        ShortLink result = shortLinkService.updateLink(code, slUpdate, token);

        assertThat(result.getExpiresAt()).isEqualTo(newInstant);
        assertThat(result.getTargetUrl()).isEqualTo(originalTargetUrl);
    }

    @Test
    void updateLink_withClearExpiry_removesTheExpiry() {

        String code = "clearExpiry";
        Instant expiry = Instant.now().plus(1, ChronoUnit.DAYS);
        String originalTargetUrl = "https://example.com";
        boolean clearExpiry = true;
        String token = "validToken";

        ShortLink link = new ShortLink(code, originalTargetUrl, expiry, hashToken);

        when(shortLinkRepository.findByCode(code)).thenReturn(Optional.of(link));

        ShortLinkUpdate slUpdate = new ShortLinkUpdate(null, null, clearExpiry);

        ShortLink result = shortLinkService.updateLink(code, slUpdate, token);

        assertThat(result.getExpiresAt()).isNull();
        assertThat(result.getTargetUrl()).isEqualTo(originalTargetUrl);
    }

    @Test
    void updateLink_withAnInvalidToken_changesNothing() {

        String code = "updateInvalidToken";
        Instant originalExpiry = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant newExpiry = Instant.now().plus(100, ChronoUnit.DAYS);
        String originalTargetUrl = "https://example.com";
        String newTargetUrl = "https://new.example.com";
        String tokenInvalid = "tokenInvalid";
        String errorMsg = "Token provided is invalid";

        ShortLink link = new ShortLink(code, originalTargetUrl, originalExpiry, hashToken);

        when(shortLinkRepository.findByCode(code)).thenReturn(Optional.of(link));

        ShortLinkUpdate slUpdate = new ShortLinkUpdate(newTargetUrl, newExpiry, false);

        doThrow(new InvalidTokenException(errorMsg, code))
                .when(authorizationTokenProcessor).authorize(link, tokenInvalid);

        assertThatThrownBy(() -> shortLinkService.updateLink(code, slUpdate, tokenInvalid))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining(errorMsg);

        assertThat(link.getTargetUrl()).isEqualTo(originalTargetUrl);
        assertThat(link.getExpiresAt()).isEqualTo(originalExpiry);
    }

    @Test
    void updateLink_withBothFields_changesTargetUrlAndExpiry() {

        String code = "updateBothFields";
        Instant newExpiry = Instant.now().plus(1, ChronoUnit.DAYS);
        String newTargetUrl = "https://new.example.com";
        String token = "tokenValid";

        ShortLink link = new ShortLink(code, "https://example.com", null, hashToken);

        when(shortLinkRepository.findByCode(code)).thenReturn(Optional.of(link));

        ShortLinkUpdate slUpdate = new ShortLinkUpdate(newTargetUrl, newExpiry, false);

        ShortLink result = shortLinkService.updateLink(code, slUpdate, token);

        assertThat(result.getTargetUrl()).isEqualTo(newTargetUrl);
        assertThat(result.getExpiresAt()).isEqualTo(newExpiry);
    }

    @Test
    void updateLink_withClearExpiryAndADate_removesTheExpiry() {

        String code = "clearExpiry";
        Instant expiry = Instant.now().plus(1, ChronoUnit.DAYS);
        String originalTargetUrl = "https://example.com";
        String token = "tokenValid";

        ShortLink link = new ShortLink(code, originalTargetUrl, expiry, hashToken);

        when(shortLinkRepository.findByCode(code)).thenReturn(Optional.of(link));

        ShortLinkUpdate slUpdate = new ShortLinkUpdate(null, expiry, true);

        ShortLink result = shortLinkService.updateLink(code, slUpdate, token);

        assertThat(result.getTargetUrl()).isEqualTo(originalTargetUrl);
        assertThat(result.getExpiresAt()).isNull();
    }


    private RedirectTarget redirectTargetExpiringAt(String url, Instant when) {

        return new RedirectTarget() {

            @Override
            public String getTargetUrl() {
                return url;
            }

            @Override
            public Instant getExpiresAt() {
                return when;
            }
        };
    }

    private void stubTokenGenerator() {
        ManagementTokenGenerator.Token mockToken = new ManagementTokenGenerator.Token("raw-token", hashToken);
        when(managementTokenGenerator.generate()).thenReturn(mockToken);
    }
}