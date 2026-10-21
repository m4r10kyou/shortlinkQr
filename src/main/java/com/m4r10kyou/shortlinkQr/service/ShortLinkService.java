package com.m4r10kyou.shortlinkQr.service;

import com.m4r10kyou.shortlinkQr.domain.ExpirationPolicy;
import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.exception.AliasAlreadyExistsException;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkExpiredException;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkNotFoundException;
import com.m4r10kyou.shortlinkQr.repository.RedirectTarget;
import com.m4r10kyou.shortlinkQr.repository.ShortLinkRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ShortLinkService {

    private final ShortLinkRepository shortLinkRepository;
    private final CodeGenerator codeGenerator;
    private final ManagementTokenGenerator tokenGenerator;
    private final AuthorizationTokenProcessor authorizationTokenProcessor;

    public ShortLinkService(ShortLinkRepository shortLinkRepository, CodeGenerator codeGenerator, ManagementTokenGenerator tokenGenerator, AuthorizationTokenProcessor authorizationTokenProcessor) {

        this.shortLinkRepository = shortLinkRepository;
        this.codeGenerator = codeGenerator;
        this.tokenGenerator = tokenGenerator;
        this.authorizationTokenProcessor = authorizationTokenProcessor;
    }

    @Transactional(readOnly = true)
    public ShortLink getByCode(String code) {

        return getShortLink(code);
    }

    @Transactional
    public CreatedShortLink createLink(String targetUrl, String customAlias, Instant expiresAt) {

        String finalCode;
        boolean usedCustomAlias = customAlias != null && !customAlias.isBlank();


        if (usedCustomAlias) {

            if (shortLinkRepository.existsByCode(customAlias)) {

                throw new AliasAlreadyExistsException( customAlias );
            }

            finalCode = customAlias;

        } else {

            finalCode = codeGenerator.generate(shortLinkRepository::existsByCode);
        }

        ManagementTokenGenerator.Token token = tokenGenerator.generate();

        ShortLink shortLink = new ShortLink(finalCode, targetUrl, expiresAt, token.tokenHash());

        try {

            return new CreatedShortLink( shortLinkRepository.save(shortLink), token.rawToken());

        } catch (DataIntegrityViolationException dive) {

            if(usedCustomAlias){

                throw new AliasAlreadyExistsException(customAlias);
            }

            throw dive;
        }
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
    public ShortLink updateLink(String code, ShortLinkUpdate shortLinkUpdate, String rawToken) {

        ShortLink shortLink = getShortLinkWithAuthorization(code,rawToken);

        if (null!= shortLinkUpdate.targetUrl()) {

            shortLink.changeTargetUrl(shortLinkUpdate.targetUrl());
        }

        if (shortLinkUpdate.clearExpiry()) {

            shortLink.removeExpiry();
        } else if(null != shortLinkUpdate.expiresAt()){

            shortLink.changeExpiry(shortLinkUpdate.expiresAt());
        }

        return shortLink;
    }

    @Transactional
    public ShortLink attachLogo(String code, byte[] sanitizedBytes, String rawToken){

        ShortLink  shortLink =  getShortLinkWithAuthorization( code,  rawToken);

        shortLink.attachLogo(sanitizedBytes);

        return shortLink;
    }

    @Transactional(readOnly = true)
    public ShortLink getByCodeWithAuthorization(String code, String rawToken){

        return getShortLinkWithAuthorization(code,rawToken);
    }

    private ShortLink getShortLink(String code){

        return shortLinkRepository.findByCode(code)
                .orElseThrow(() -> new ShortLinkNotFoundException(code));
    }

    private ShortLink getShortLinkWithAuthorization(String code, String rawToken){

        ShortLink shortLink = getShortLink(code);

        authorizationTokenProcessor.authorize(shortLink,rawToken);

        return shortLink;
    }
}
