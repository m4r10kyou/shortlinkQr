package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.dto.ShortLinkRequest;
import com.m4r10kyou.shortlinkQr.dto.ShortLinkResponse;
import com.m4r10kyou.shortlinkQr.service.QrCodeGenerator;
import com.m4r10kyou.shortlinkQr.service.ShortLinkService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/links")
public class ShortLinkController {

    private final ShortLinkService shortLinkService;
    private final String baseUrl;

    private final QrCodeGenerator qrGenerator;

    public ShortLinkController(ShortLinkService shortLinkService, @Value("${shortlink.base-url}") String url, QrCodeGenerator qrGenerator) {
        this.shortLinkService = shortLinkService;
        this.baseUrl = url.endsWith("/") ? url : url + "/";
        this.qrGenerator = qrGenerator;
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

    @GetMapping(value = "/{code}/qr",
                produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(@PathVariable("code") String code){

        ShortLink shortLink =shortLinkService.getByCode(code);
        String url = this.baseUrl + shortLink.getCode();
        byte[] qrImage = qrGenerator.generate(url);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).immutable())
                .body(qrImage);
    }

}
