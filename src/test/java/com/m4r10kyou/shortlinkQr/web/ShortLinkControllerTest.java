package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.exception.InvalidLogoException;
import com.m4r10kyou.shortlinkQr.exception.InvalidTokenException;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkNotFoundException;
import com.m4r10kyou.shortlinkQr.service.CreatedShortLink;
import com.m4r10kyou.shortlinkQr.service.LogoImageProcessor;
import com.m4r10kyou.shortlinkQr.service.QrCodeGenerator;
import com.m4r10kyou.shortlinkQr.service.ShortLinkService;
import com.m4r10kyou.shortlinkQr.service.ShortLinkUpdate;
import com.m4r10kyou.shortlinkQr.validation.LogoUploadValidator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShortLinkController.class,
        properties = "shortlink.base-url=http://short.test")
class ShortLinkControllerTest {

    private final String hashToken = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShortLinkService shortLinkService;

    @MockitoBean
    private QrCodeGenerator qrCodeGenerator;

    @MockitoBean
    private LogoImageProcessor logoImageProcessor;

    @MockitoBean
    private LogoUploadValidator logoUploadValidator;


    @Test
    void createLink_withValidRequest_returns201AndTrimsUrl() throws Exception {

        String json = """
                {
                    "targetUrl": " https://example.com ",
                    "customAlias": "mi-repo"
                }
                """;

        ShortLink unShortLink = new ShortLink("mi-repo", "https://example.com",null,hashToken);
        CreatedShortLink createdShortLink = new CreatedShortLink(unShortLink,"raw-token");

        when(shortLinkService.createLink(any(), any(), any())).thenReturn(createdShortLink);

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value("http://short.test/mi-repo"));

        verify(shortLinkService).createLink(eq("https://example.com"), eq("mi-repo"), isNull());

    }

    @Test
    void createLink_withNonHttpScheme_returns400AndNeverCallsService() throws Exception {

        String json = """
                {
                    "targetUrl": "mailto:someone@email.com"
                }
                """;

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.targetUrl").exists());

        verify(shortLinkService, never()).createLink(any(), any(), any());
    }

    @Test
    void getShortLink_withUnknownCode_returns404WithFailingCode() throws Exception {

        when(shortLinkService.getByCodeWithAuthorization("notFound","rawToken")).thenThrow(new ShortLinkNotFoundException("notFound"));

        mockMvc.perform(get("/api/links/notFound")
                        .header("X-Management-Token","rawToken"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem-type:not-found"))
                .andExpect(jsonPath("$.code").value("notFound"));
    }

    @Test
    void uploadLogo_withValidRequest_returns200WithTheUpdatedLink() throws Exception {

        ShortLink shortLink = new ShortLink("mi-repo", "https://example.com",null,hashToken);
        String code = "mi-repo";
        String token= "validToken";
        byte[] originalBytes = "contenido-original".getBytes();
        byte[] sanitizedBytes = "contenido-saneado".getBytes();

        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "avatar.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                originalBytes
        );

        shortLink.attachLogo(sanitizedBytes);

        when(logoImageProcessor.sanitize(any(byte[].class))).thenReturn(sanitizedBytes);
        when(shortLinkService.attachLogo(eq(code), any(byte[].class),anyString()))
                .thenReturn(shortLink);

        mockMvc.perform( MockMvcRequestBuilders
                        .multipart("/api/links/{code}/logo",code)
                        .header("X-Management-Token", token)
                        .file(mockFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasLogo").value(true))
                .andExpect(jsonPath("$.managementToken").doesNotExist());


        verify(shortLinkService).attachLogo(code, sanitizedBytes,token);
    }

    @Test
    void uploadLogo_withAnInvalidFile_returns400() throws Exception {

        String code = "invalid-logo";
        byte[] originalBytes = "contenido-original".getBytes();

        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "avatar.txt",
                MediaType.TEXT_PLAIN_VALUE,
                originalBytes
        );

        when(logoImageProcessor.sanitize(any(byte[].class)))
                .thenThrow(new InvalidLogoException("Unsupported image format"));

        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/api/links/{code}/logo", code)
                        .file(mockFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:invalid-logo"));

    }

    @Test
    void uploadLogo_withNotFoundCode_returns404() throws Exception {

        String code = "notFound";
        byte[] originalBytes = "contenido-original".getBytes();
        byte[] sanitizedBytes = "contenido-saneado".getBytes();

        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "avatar.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                originalBytes
        );

        when(logoImageProcessor.sanitize(any(byte[].class))).thenReturn(sanitizedBytes);

        when(shortLinkService.attachLogo(eq(code), any(byte[].class),anyString()))
                .thenThrow(new ShortLinkNotFoundException("notFound"));

        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/api/links/{code}/logo", code)
                        .header("X-Management-Token","rawToken")
                        .file(mockFile))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem-type:not-found"))
                .andExpect(jsonPath("$.code").value("notFound"));

    }

    @Test
    void uploadLogo_withoutTheFilePart_returns400() throws Exception {

        String code = "mi-repo";

        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/api/links/{code}/logo", code))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getQrCode_withALinkThatHasALogo_returns200AndUsesTheBrandedGenerator() throws Exception {

        ShortLink shortLink = new ShortLink("has-logo", "https://example.com",null,hashToken);

        byte[] logoBytes = new byte[] { 1, 2, 3 };
        shortLink.attachLogo(logoBytes);

        byte[] brandedQr = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 };

        String expectedUrl = "http://short.test/has-logo";

        when(shortLinkService.getByCode(shortLink.getCode())).thenReturn(shortLink);

        when(qrCodeGenerator.generateWithLogo(expectedUrl, logoBytes)).thenReturn(brandedQr);

        mockMvc.perform(get("/api/links/{code}/qr", shortLink.getCode()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE))
                .andExpect(content().bytes(brandedQr))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=300")));

        verify(qrCodeGenerator, never()).generate(anyString());
        verify(qrCodeGenerator).generateWithLogo(expectedUrl, logoBytes);
        verify(shortLinkService, never()).resolveCode(any());
    }

    @Test
    void getQrCode_withALinkWithoutALogo_returns200AndNeverUsesTheBrandedGenerator() throws Exception {

        ShortLink shortLink = new ShortLink("no-logo", "https://example.com",null,hashToken);

        byte[] normalQr = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 };

        String expectedUrl = "http://short.test/no-logo";

        when(shortLinkService.getByCode(shortLink.getCode())).thenReturn(shortLink);

        when(qrCodeGenerator.generate(expectedUrl)).thenReturn(normalQr);

        mockMvc.perform(get("/api/links/{code}/qr", shortLink.getCode()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE))
                .andExpect(content().bytes(normalQr))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=300")));

        verify(qrCodeGenerator).generate(expectedUrl);
        verify(qrCodeGenerator, never()).generateWithLogo(anyString(), any(byte[].class));
        verify(shortLinkService, never()).resolveCode(any());
    }

    @Test
    void createLink_withValidRequest_returns201AndHasManagementToken() throws Exception{

        String json = """
                {
                    "targetUrl": " https://example.com ",
                    "customAlias": "tokenmng-repo"
                }
                """;

        ShortLink shortLinkWithToken = new ShortLink("tokenmng-repo", "https://example.com",null,hashToken);
        CreatedShortLink createdShortLink = new CreatedShortLink(shortLinkWithToken, "raw-token" );

        when(shortLinkService.createLink(any(), any(), any())).thenReturn(createdShortLink);

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.managementToken").value("raw-token"));
    }

    @Test
    void getShortLink_withValidRequest_returns200WithoutTheManagementToken() throws Exception{

        String code ="my-sl-no-token";
        String token = "raw-token";
        ShortLink getSLnoMngToken = new ShortLink(code, "https://example.com",null,hashToken);

        when(shortLinkService.getByCodeWithAuthorization(code, token)).thenReturn(getSLnoMngToken);

        mockMvc.perform(get("/api/links/{code}", code)
                        .header("X-Management-Token",token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managementToken").doesNotExist())
                .andExpect(jsonPath("$.shortUrl").value("http://short.test/my-sl-no-token"))
                .andExpect(jsonPath("$.targetUrl").value("https://example.com"));
    }

    @Test
    void uploadLogo_withoutManagementToken_returns403() throws Exception {

        String code = "mi-repo-forbidden";
        byte[] originalBytes = "contenido-original".getBytes();
        byte[] sanitizedBytes = "contenido-saneado".getBytes();

        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "avatar.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                originalBytes
        );

        when(logoImageProcessor.sanitize(any(byte[].class))).thenReturn(sanitizedBytes);

        when(shortLinkService.attachLogo(eq(code), any(byte[].class),nullable(String.class)))
                .thenThrow(new InvalidTokenException("Management token is missing",code));

        mockMvc.perform(MockMvcRequestBuilders
                        .multipart("/api/links/{code}/logo", code)
                        .file(mockFile))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type")
                        .value("urn:problem-type:invalid-management-token"));
    }

    @Test
    void getShortLink_withAnInvalidManagementToken_returns403() throws Exception {
        String code ="my-sl-no-token";
        String invalidToken = "that-is-invalid";

        when(shortLinkService.getByCodeWithAuthorization(code, invalidToken))
                .thenThrow(new InvalidTokenException("Management token is missing",code));

        mockMvc.perform(get("/api/links/{code}", code)
                        .header("X-Management-Token",invalidToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type")
                        .value("urn:problem-type:invalid-management-token"));
    }

    @Test
    void updateLink_withNoFields_returns400AndNeverCallsService() throws Exception {

        String code ="empty-body";
        String errorMsg = "Can not edit Short Link without parameters";
        String json = "{}";

        mockMvc.perform(patch("/api/links/{code}", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.notEmptyRequest").value(errorMsg));

        verify(shortLinkService, never()).updateLink(any(), any(), any());
    }

    @Test
    void updateLink_withBothExpiryFields_returns400AndNeverCallsService() throws Exception {

        String code ="both-expiry";
        Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS);
        String json = """
                {
                    "expiresAt"     : "%s",
                    "clearExpiry"   : true
                }
                """.formatted(expiresAt);

        mockMvc.perform(patch("/api/links/{code}", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());

        verify(shortLinkService, never()).updateLink(any(), any(), any());
    }

    @Test
    void updateLink_withAValidRequest_returns200AndTheUpdatedLink() throws Exception {

        String code ="changeUrl";
        String token = "validToken";
        String newTargetUrl = "https://new.example.com";
        String json = """
                {
                    "targetUrl": "https://new.example.com"
                }
                """;

        ShortLink updatedShortLink = new ShortLink(code, newTargetUrl, null, hashToken);

        when(shortLinkService.updateLink(eq(code), any(ShortLinkUpdate.class), eq(token)))
                .thenReturn(updatedShortLink);

        mockMvc.perform(patch("/api/links/{code}", code)
                        .header("X-Management-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetUrl").value(newTargetUrl))
                .andExpect(jsonPath("$.managementToken").doesNotExist());

        ArgumentCaptor<ShortLinkUpdate> updateCaptor = ArgumentCaptor.forClass(ShortLinkUpdate.class);
        verify(shortLinkService).updateLink(eq(code), updateCaptor.capture(), eq(token));

        ShortLinkUpdate capturedUpdate = updateCaptor.getValue();
        assertThat(capturedUpdate.targetUrl()).isEqualTo(newTargetUrl);
        assertThat(capturedUpdate.expiresAt()).isNull();
        assertThat(capturedUpdate.clearExpiry()).isFalse();
    }

    @Test
    void updateLink_withChangeUrlAndClearExpiry_returns200AndTheUpdatedLink() throws Exception {

        String code ="changeUrl-clearExpity";
        String token = "validToken";
        String newTargetUrl = "https://new.example.com";
        String json = """
                {
                    "targetUrl"     : "%s",
                    "clearExpiry"   : true
                }
                """.formatted(newTargetUrl);

        ShortLink updatedShortLink = new ShortLink(code, newTargetUrl, null, hashToken);

        when(shortLinkService.updateLink(eq(code), any(ShortLinkUpdate.class), eq(token)))
                .thenReturn(updatedShortLink);

        mockMvc.perform(patch("/api/links/{code}", code)
                        .header("X-Management-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetUrl").value(newTargetUrl))
                .andExpect(jsonPath("$.managementToken").doesNotExist());

        ArgumentCaptor<ShortLinkUpdate> updateCaptor = ArgumentCaptor.forClass(ShortLinkUpdate.class);
        verify(shortLinkService).updateLink(eq(code), updateCaptor.capture(), eq(token));

        ShortLinkUpdate capturedUpdate = updateCaptor.getValue();
        assertThat(capturedUpdate.targetUrl()).isEqualTo(newTargetUrl);
        assertThat(capturedUpdate.expiresAt()).isNull();
        assertThat(capturedUpdate.clearExpiry()).isTrue();
    }

    @Test
    void updateLink_withoutManagementToken_returns403() throws Exception {

        String code = "update-forbidden";
        String json = """
                {
                    "targetUrl": "https://no-update.example.com"
                }
                """;

        when(shortLinkService.updateLink(eq(code), any(ShortLinkUpdate.class), nullable(String.class)))
                .thenThrow(new InvalidTokenException("Management token is missing",code));


        mockMvc.perform(patch("/api/links/{code}", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type")
                        .value("urn:problem-type:invalid-management-token"));
    }

    @Test
    void updateLink_withABlankTargetUrl_returns400() throws Exception {

        String code = "blank-target";
        String json = """
                        {
                            "targetUrl": ""
                        }
                        """;

        mockMvc.perform(patch("/api/links/{code}", code)
                        .header("X-Management-Token", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.targetUrl").exists());

        verify(shortLinkService, never()).updateLink(any(), any(), any());
    }

    @Test
    void updateLink_withNonHttpScheme_returns400() throws Exception {

        String code = "no-http";
        String noHttp ="mailto:someone@email.com";
        String json = """
                        {
                            "targetUrl": "%s"
                        }
                        """.formatted(noHttp);

        mockMvc.perform(patch("/api/links/{code}", code)
                        .header("X-Management-Token", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.targetUrl").exists());

        verify(shortLinkService, never()).updateLink(any(), any(), any());
    }

    @Test
    void updateLink_withAPastExpiry_returns400() throws Exception {

        String code = "past-expiry";
        String json = """
                        {
                           "expiresAt" : "2019-01-21T23:30:45Z"
                        }
                        """;

        mockMvc.perform(patch("/api/links/{code}", code)
                        .header("X-Management-Token", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.expiresAt").exists());

        verify(shortLinkService, never()).updateLink(any(), any(), any());
    }

    @Test
    void updateLink_withAnUnreadableBody_returns400() throws Exception {

        String code = "no-body";


        mockMvc.perform(patch("/api/links/{code}", code)
                        .header("X-Management-Token", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").doesNotExist());

        verify(shortLinkService, never()).updateLink(any(), any(), any());
    }

}
