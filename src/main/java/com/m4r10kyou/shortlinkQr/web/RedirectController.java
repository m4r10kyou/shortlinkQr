package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.domain.ShortLinkConstraints;
import com.m4r10kyou.shortlinkQr.service.ShortLinkService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URI;

@Controller
public class RedirectController {

    private final ShortLinkService shortLinkService;

    public RedirectController(ShortLinkService shortLinkService) {
        this.shortLinkService = shortLinkService;
    }

    @GetMapping("/{code:"+ ShortLinkConstraints.CODE_ALLOWED_CHARS
    + "{"+ShortLinkConstraints.CODE_MIN_LENGTH +","
    + ShortLinkConstraints.CODE_MAX_LENGTH +"}}")
    public ResponseEntity<Void> redirect(@PathVariable("code") String code){

        String targetUrl = shortLinkService.resolveCode(code);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(targetUrl))
                .cacheControl(CacheControl.noCache())
                .build();
    }
}
