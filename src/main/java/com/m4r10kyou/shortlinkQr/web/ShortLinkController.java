package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.dto.ShortLinkCreationResponse;
import com.m4r10kyou.shortlinkQr.dto.ShortLinkEditionRequest;
import com.m4r10kyou.shortlinkQr.dto.ShortLinkRequest;
import com.m4r10kyou.shortlinkQr.dto.ShortLinkResponse;
import com.m4r10kyou.shortlinkQr.service.CreatedShortLink;
import com.m4r10kyou.shortlinkQr.service.LogoImageProcessor;
import com.m4r10kyou.shortlinkQr.service.QrCodeGenerator;
import com.m4r10kyou.shortlinkQr.service.ShortLinkService;
import com.m4r10kyou.shortlinkQr.service.ShortLinkUpdate;
import com.m4r10kyou.shortlinkQr.validation.LogoUploadValidator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

@RestController
@RequestMapping("/api/links")
public class ShortLinkController {

    private final ShortLinkService shortLinkService;
    private final String baseUrl;

    private final QrCodeGenerator qrGenerator;

    private final LogoImageProcessor logoImageProcessor;
    private final LogoUploadValidator logoUploadValidator;

    public ShortLinkController(ShortLinkService shortLinkService,
                               @Value("${shortlink.base-url}") String url,
                               QrCodeGenerator qrGenerator,
                               LogoImageProcessor logoImageProcessor,
                               LogoUploadValidator logoUploadValidator) {

        this.shortLinkService = shortLinkService;
        this.baseUrl = url.endsWith("/") ? url : url + "/";
        this.qrGenerator = qrGenerator;
        this.logoImageProcessor = logoImageProcessor;
        this.logoUploadValidator = logoUploadValidator;
    }

    @GetMapping("/{code}")
    public ShortLinkResponse getShortLink(@PathVariable("code") String code,
                                          @RequestHeader(required = false , name ="X-Management-Token")String rawToken) {

        ShortLink shortLink = shortLinkService.getByCodeWithAuthorization(code, rawToken);

        return ShortLinkResponse.from(shortLink, baseUrl)  ;
    }

    @PostMapping
    public ResponseEntity<ShortLinkCreationResponse> createShortLink(@Valid @RequestBody ShortLinkRequest request){

        CreatedShortLink createdShortLink = shortLinkService.createLink(request.targetUrl(), request.customAlias(), request.expiresAt());

        ShortLinkCreationResponse bodyResponse = ShortLinkCreationResponse.from(createdShortLink.shortLink(), baseUrl, createdShortLink.rawToken() );

        return  ResponseEntity
                .created(URI.create(bodyResponse.shortUrl()))
                .cacheControl(CacheControl.noStore())
                .body(bodyResponse);
    }

    @PatchMapping("/{code}")
    public ShortLinkResponse editShortLink(@PathVariable("code") String code,
                                           @RequestHeader(required = false , name ="X-Management-Token")String rawToken,
                                           @Valid @RequestBody ShortLinkEditionRequest shortLink) {


        ShortLinkUpdate shortLinkChangeRequest = new ShortLinkUpdate(shortLink.targetUrl(),
                                                                    shortLink.expiresAt(),
                                                                    shortLink.clearExpiry());

        ShortLink shortLinkUpdated = shortLinkService.updateLink(code, shortLinkChangeRequest, rawToken);

        return ShortLinkResponse.from(shortLinkUpdated, baseUrl);
    }

    @GetMapping(value = "/{code}/qr",
                produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(@PathVariable("code") String code){

        ShortLink shortLink =shortLinkService.getByCode(code);

        byte[] qrImage = generateQrImage(shortLink);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
                .body(qrImage);
    }

    @PostMapping(value = "/{code}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ShortLinkResponse uploadLogo(@PathVariable("code") String code,
                                        @RequestParam("file") MultipartFile file,
                                        @RequestHeader(required = false , name ="X-Management-Token")String rawToken) throws IOException {

        logoUploadValidator.validate(file.getSize(), file.getContentType());

        shortLinkService.getByCodeWithAuthorization(code,rawToken);

        byte[] imgBytes = file.getBytes();

        byte[] sanitizedImgBytes = logoImageProcessor.sanitize(imgBytes);

        ShortLink shortLink = shortLinkService.attachLogo(code, sanitizedImgBytes,rawToken);

        return ShortLinkResponse.from(shortLink, baseUrl);
    }

    private byte[] generateQrImage(ShortLink shortLink){

        String url = this.baseUrl + shortLink.getCode();

        if(shortLink.hasLogo()){

            return qrGenerator.generateWithLogo(url, shortLink.getLogoData());
        }

        return  qrGenerator.generate(url);
    }

}
