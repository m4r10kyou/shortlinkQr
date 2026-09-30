package com.m4r10kyou.shortlinkQr.service;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.exception.AliasAlreadyExistsException;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkExpiredException;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkNotFoundException;
import com.m4r10kyou.shortlinkQr.repository.ShortLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ShortLinkService {

    private final ShortLinkRepository shortLinkRepository;
    private final CodeGenerator codeGenerator;

    public ShortLinkService(ShortLinkRepository shortLinkRepository, CodeGenerator codeGenerator) {

        this.shortLinkRepository = shortLinkRepository;
        this.codeGenerator = codeGenerator;
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

        ShortLink shortLink = new ShortLink(finalCode, targetUrl, expiresAt);
        return shortLinkRepository.save(shortLink);
    }

    @Transactional
    public String resolveCode(String code) {

        ShortLink  shortLink = this.getByCode(code);

        if (shortLink.isExpired()) {
            throw new ShortLinkExpiredException(code);
        }

        shortLinkRepository.registerVisitCount(code);

        return shortLink.getTargetUrl();
    }

    @Transactional
    public ShortLink updateDestination(String code, String newTargetUrl) {

        ShortLink  shortLink = this.getByCode(code);

        shortLink.changeTargetUrl(newTargetUrl);
        return shortLink;
    }
}
