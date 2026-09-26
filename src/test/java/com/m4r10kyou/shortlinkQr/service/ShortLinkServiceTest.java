package com.m4r10kyou.shortlinkQr.service;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.exception.AliasAlreadyExistsException;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkExpiredException;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkNotFoundException;
import com.m4r10kyou.shortlinkQr.repository.ShortLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private CodeGenerator codeGenerator;

    @InjectMocks
    private ShortLinkService shortLinkService;

    @Test
    void createLink_withFreeAlias_usesAliasAndDoesNotCallGenerator() {

        String target = "https://example.com";
        String alias = "my-alias";

        when(shortLinkRepository.existsByCode(alias)).thenReturn(false);

        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortLink result = shortLinkService.createLink(target, alias, null);

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
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortLink result = shortLinkService.createLink(target, null, null);

        assertThat(result.getCode()).isEqualTo(generatedCode);

        verify(codeGenerator).generate(any());
        verify(shortLinkRepository).save(any(ShortLink.class));
    }


    @Test
    void resolveCode_withValidCode_returnsTargetUrlAndCountsVisit() {

        String target = "https://example.com";
        String code = "valid";
        ShortLink link = new ShortLink(code, target, null);

        when(shortLinkRepository.findByCode(code)).thenReturn(Optional.of(link));

        String result = shortLinkService.resolveCode(code);

        assertThat(result).isEqualTo(target);

        verify(shortLinkRepository).registerVisitCount(code);
    }

    @Test
    void resolveCode_withNotFoundCode_throwsShortLinkNotFoundException() {

        String code = "not-exist";

        when(shortLinkRepository.findByCode(code)).thenReturn(Optional.empty());

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
        ShortLink link = new ShortLink(code, target, pastExpiration);

        when(shortLinkRepository.findByCode(code)).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> shortLinkService.resolveCode(code))
                .isInstanceOf(ShortLinkExpiredException.class)
                .hasMessageContaining(code);

        verify(shortLinkRepository, never()).registerVisitCount(any());
    }

    @Test
    void updateDestination_withNotFoundCode_throwsShortLinkNotFoundException() {

        String code = "not-exist-code-target-url";
        String target = "https://false.example.com";

        when(shortLinkRepository.findByCode(code)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shortLinkService.updateDestination(code,target))
                .isInstanceOf(ShortLinkNotFoundException.class)
                .hasMessageContaining(code);

        verify(shortLinkRepository, never()).save(any());

    }

    @Test
    void updateDestination_withValidCode_changesTargetUrl() {

        String code = "newTargetUrl";
        String oldTarget = "https://example.com";
        String newTarget = "https://new.example.com";
        ShortLink link = new ShortLink(code, oldTarget, null);

        when(shortLinkRepository.findByCode(code)).thenReturn(Optional.of(link));

        ShortLink result = shortLinkService.updateDestination(code, newTarget);

        assertThat(result.getTargetUrl()).isEqualTo(newTarget);
    }
}