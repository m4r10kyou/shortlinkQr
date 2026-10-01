package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.exception.ShortLinkExpiredException;
import com.m4r10kyou.shortlinkQr.service.ShortLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RedirectController.class,
        properties = "shortlink.base-url=http://short.test")
public class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShortLinkService shortLinkService;

    @Test
    void redirect_existingCode_returns302WithLocationHeader() throws Exception {

        when(shortLinkService.resolveCode("mi-repo"))
                .thenReturn("https://example.com");

        mockMvc.perform(get("/mi-repo"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"));
    }

    @Test
    void redirect_expiredCode_returns410WithFailingCode() throws Exception {

        when(shortLinkService.resolveCode("expired"))
                .thenThrow(new ShortLinkExpiredException("expired"));

        mockMvc.perform(get("/expired"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.type").value("urn:problem-type:link-expired"))
                .andExpect(jsonPath("$.code").value("expired"));
    }
}
