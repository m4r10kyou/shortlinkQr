package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.exception.InvalidLogoException;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkNotFoundException;
import com.m4r10kyou.shortlinkQr.service.CreatedShortLink;
import com.m4r10kyou.shortlinkQr.service.LogoImageProcessor;
import com.m4r10kyou.shortlinkQr.service.QrCodeGenerator;
import com.m4r10kyou.shortlinkQr.service.ShortLinkService;
import com.m4r10kyou.shortlinkQr.validation.LogoUploadValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        when(shortLinkService.getByCode("notFound")).thenThrow(new ShortLinkNotFoundException("notFound"));

        mockMvc.perform(get("/api/links/notFound"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem-type:not-found"))
                .andExpect(jsonPath("$.code").value("notFound"));
    }

    @Test
    void uploadLogo_withValidRequest_returns200WithTheUpdatedLink() throws Exception {

        ShortLink shortLink = new ShortLink("mi-repo", "https://example.com",null,hashToken);
        String code = "mi-repo";
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
        when(shortLinkService.attachLogo(eq(code), any(byte[].class)))
                .thenReturn(shortLink);

        mockMvc.perform( MockMvcRequestBuilders
                    .multipart("/api/links/{code}/logo",code)
                    .file(mockFile))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasLogo").value(true))
                    .andExpect(jsonPath("$.managementToken").doesNotExist());


        verify(shortLinkService).attachLogo(code, sanitizedBytes);
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

        when(shortLinkService.attachLogo(eq(code), any(byte[].class)))
                .thenThrow(new ShortLinkNotFoundException("notFound"));

        mockMvc.perform(MockMvcRequestBuilders
                .multipart("/api/links/{code}/logo", code)
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
        ShortLink getSLnoMngToken = new ShortLink(code, "https://example.com",null,hashToken);

        when(shortLinkService.getByCode(code)).thenReturn(getSLnoMngToken);

        mockMvc.perform(get("/api/links/{code}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managementToken").doesNotExist())
                .andExpect(jsonPath("$.shortUrl").value("http://short.test/my-sl-no-token"))
                .andExpect(jsonPath("$.targetUrl").value("https://example.com"));

    }

}
