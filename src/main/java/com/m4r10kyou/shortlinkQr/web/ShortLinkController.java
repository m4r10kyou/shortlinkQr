package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.dto.ShortLinkRequest;
import com.m4r10kyou.shortlinkQr.dto.ShortLinkResponse;
import com.m4r10kyou.shortlinkQr.service.ShortLinkService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/links")
public class ShortLinkController {

    private final ShortLinkService shortLinkService;
    private final String baseUrl;

    public ShortLinkController(ShortLinkService shortLinkService, @Value("${shortlink.base-url}") String url) {
        this.shortLinkService = shortLinkService;
        this.baseUrl = url;
    }

    @GetMapping("/{code}")
    public ShortLinkResponse getShortLink(@PathVariable("code") String code) {

        ShortLink shortLink = shortLinkService.getByCode(code);

        return ShortLinkResponse.from(shortLink, baseUrl)  ;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShortLinkResponse createShortLink(@Valid @RequestBody ShortLinkRequest request){

        ShortLink shortLink = shortLinkService.createLink(request.targetUrl(), request.customAlias(), request.expiresAt());

        return ShortLinkResponse.from(shortLink, baseUrl )  ;
    }

}
