package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkNotFoundException;
import com.m4r10kyou.shortlinkQr.service.QrCodeGenerator;
import com.m4r10kyou.shortlinkQr.service.ShortLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
public class ShortLinkControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ShortLinkService shortLinkService;

    @MockitoBean
    QrCodeGenerator qrCodeGenerator;

    @Test
    void createLink_validRequest_returns201AndTrimsUrl() throws Exception {

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
    void getQrCode_validRequest_returns200WithPngImage() throws Exception {

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
}
