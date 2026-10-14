package com.m4r10kyou.shortlinkQr.service;

import com.m4r10kyou.shortlinkQr.domain.ExpirationPolicy;
import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.exception.AliasAlreadyExistsException;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkExpiredException;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkNotFoundException;
import com.m4r10kyou.shortlinkQr.repository.RedirectTarget;
import com.m4r10kyou.shortlinkQr.repository.ShortLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ShortLinkService {

    private final ShortLinkRepository shortLinkRepository;
    private final CodeGenerator codeGenerator;
    private final ManagementTokenGenerator tokenGenerator;

    public ShortLinkService(ShortLinkRepository shortLinkRepository, CodeGenerator codeGenerator, ManagementTokenGenerator tokenGenerator) {

        this.shortLinkRepository = shortLinkRepository;
        this.codeGenerator = codeGenerator;
        this.tokenGenerator = tokenGenerator;
    }

    @Transactional(readOnly = true)
    public ShortLink getByCode(String code) {

        return shortLinkRepository.findByCode(code)
                .orElseThrow(() -> new ShortLinkNotFoundException(code));
    }

    @Transactional
    public ShortLink createLink(String targetUrl, String customAlias, Instant expiresAt) {

        String finalCode;

        if (customAlias != null && !customAlias.isBlank()) {

            if (shortLinkRepository.existsByCode(customAlias)) {

                throw new AliasAlreadyExistsException( customAlias );
            }

            finalCode = customAlias;

        } else {

            finalCode = codeGenerator.generate(shortLinkRepository::existsByCode);
        }

        ManagementTokenGenerator.Token token = tokenGenerator.generate();

        ShortLink shortLink = new ShortLink(finalCode, targetUrl, expiresAt, token.tokenHash());
        return shortLinkRepository.save(shortLink);
    }

    @Transactional
    public String resolveCode(String code) {

        RedirectTarget target = shortLinkRepository.findTargetByCode(code)
                .orElseThrow( () -> new ShortLinkNotFoundException(code));


        if (ExpirationPolicy.isExpired(target.getExpiresAt())) {
            throw new ShortLinkExpiredException(code);
        }

        shortLinkRepository.registerVisitCount(code);

        return target.getTargetUrl();
    }

    @Transactional
    public ShortLink updateDestination(String code, String newTargetUrl) {

        ShortLink  shortLink = this.getByCode(code);

        shortLink.changeTargetUrl(newTargetUrl);
        return shortLink;
    }

    @Transactional
    public ShortLink attachLogo(String code, byte[] sanitizedBytes){

        ShortLink  shortLink = this.getByCode(code);

        shortLink.attachLogo(sanitizedBytes);

        return shortLink;
    }
}
