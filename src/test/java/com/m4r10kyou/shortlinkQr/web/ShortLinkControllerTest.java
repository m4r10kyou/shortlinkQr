package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.exception.InvalidLogoException;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkNotFoundException;
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

        ShortLink unShortLink = new ShortLink("mi-repo", "https://example.com",null);

        when(shortLinkService.createLink(any(), any(), any())).thenReturn(unShortLink);

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
    void getQrCode_withValidRequest_returns200WithPngImage() throws Exception {

        ShortLink shortLink = new ShortLink("mi-repo", "https://example.com",null);

        byte[] dummyImage = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 };

        String expectedUrl = "http://short.test/mi-repo";

        when(shortLinkService.getByCode(shortLink.getCode())).thenReturn(shortLink);
        when(qrCodeGenerator.generate(expectedUrl)).thenReturn(dummyImage);

        mockMvc.perform(get("/api/links/{code}/qr", shortLink.getCode()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE))
                .andExpect(content().bytes(dummyImage))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age=31536000")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("immutable")));

        verify(qrCodeGenerator).generate(expectedUrl);
        verify(shortLinkService, never()).resolveCode(any());
    }

    @Test
    void uploadLogo_withValidRequest_returns200WithTheUpdatedLink() throws Exception {

        ShortLink shortLink = new ShortLink("mi-repo", "https://example.com",null);
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
                    .andExpect(jsonPath("$.hasLogo").value(true));

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
}
