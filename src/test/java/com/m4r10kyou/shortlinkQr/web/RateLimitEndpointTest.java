package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.config.WebMvcConfig;
import com.m4r10kyou.shortlinkQr.domain.ShortLink;
import com.m4r10kyou.shortlinkQr.service.CreatedShortLink;
import com.m4r10kyou.shortlinkQr.service.LogoImageProcessor;
import com.m4r10kyou.shortlinkQr.service.QrCodeGenerator;
import com.m4r10kyou.shortlinkQr.service.ShortLinkService;
import com.m4r10kyou.shortlinkQr.validation.LogoUploadValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShortLinkController.class,
        properties = {
                        "shortlink.rate-limit.requests=2",
                        "shortlink.rate-limit.window-seconds=60"
})
@ActiveProfiles("test")
@Import({RateLimitInterceptor.class , WebMvcConfig.class})
class RateLimitEndpointTest {

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
    void createShortLink_whenTheLimitIsExceeded_returns429WithRetryAfter() throws Exception {

        String json = """
                {
                    "targetUrl": "https://example.com",
                    "customAlias": "mi-repo"
                }
                """;

        ShortLink unShortLink = new ShortLink("mi-repo", "https://example.com", null, hashToken);
        CreatedShortLink createdShortLink = new CreatedShortLink(unShortLink, "raw-token");

        when(shortLinkService.createLink(any(), any(), any())).thenReturn(createdShortLink);

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"))
                .andExpect(jsonPath("$.type").value("urn:problem-type:rate-limit-exceeded"));
        ;
    }

}