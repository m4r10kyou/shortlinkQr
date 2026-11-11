package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.domain.ShortLinkConstraints;
import com.m4r10kyou.shortlinkQr.service.ShortLinkService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.net.URI;

@Controller
public class RedirectController {

    private static final String CODE_PATTERN = "/{code:"+ ShortLinkConstraints.CODE_ALLOWED_CHARS
            + "{"+ShortLinkConstraints.CODE_MIN_LENGTH +","
            + ShortLinkConstraints.CODE_MAX_LENGTH +"}}";

    private final ShortLinkService shortLinkService;

    public RedirectController(ShortLinkService shortLinkService) {
        this.shortLinkService = shortLinkService;
    }

    @GetMapping(CODE_PATTERN)
    public ResponseEntity<Void> redirect(@PathVariable("code") String code){

        String targetUrl = shortLinkService.resolveCode(code);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(targetUrl))
                .cacheControl(CacheControl.noCache())
                .build();
    }

    @RequestMapping(value=CODE_PATTERN, method = RequestMethod.HEAD)
    public ResponseEntity<Void> redirectHead(@PathVariable("code") String code){

        String targetUrl = shortLinkService.lookupTargetWithoutCounting(code);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(targetUrl))
                .cacheControl(CacheControl.noCache())
                .build();
    }
}
