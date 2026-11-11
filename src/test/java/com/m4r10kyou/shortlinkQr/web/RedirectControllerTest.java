package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.exception.ShortLinkExpiredException;
import com.m4r10kyou.shortlinkQr.service.ShortLinkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RedirectController.class, properties = "shortlink.base-url=http://short.test")
@ActiveProfiles("test")
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShortLinkService shortLinkService;

    @Test
    void redirect_withExistingCode_returns302WithLocationHeader() throws Exception {

        when(shortLinkService.resolveCode("mi-repo")).thenReturn("https://example.com");

        mockMvc.perform(get("/mi-repo")).andExpect(status().isFound()).andExpect(header().string(HttpHeaders.LOCATION, "https://example.com")).andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"));

        verify(shortLinkService).resolveCode("mi-repo");
        verify(shortLinkService, never()).lookupTargetWithoutCounting("mi-repo");
    }

    @Test
    void redirect_withExpiredCode_returns410WithFailingCode() throws Exception {

        when(shortLinkService.resolveCode("expired")).thenThrow(new ShortLinkExpiredException("expired"));

        mockMvc.perform(get("/expired")).andExpect(status().isGone()).andExpect(jsonPath("$.type").value("urn:problem-type:link-expired")).andExpect(jsonPath("$.code").value("expired"));

        verify(shortLinkService).resolveCode("expired");
        verify(shortLinkService, never()).lookupTargetWithoutCounting("expired");

    }

    @Test
    void head_withExistingCode_returns302WithoutCountingTheVisit() throws Exception {

        String code = "mi-repo";

        when(shortLinkService.lookupTargetWithoutCounting(code)).thenReturn("https://example.com");

        mockMvc.perform(head("/{code}", code)).andExpect(status().isFound()).andExpect(header().string(HttpHeaders.LOCATION, "https://example.com")).andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"));

        verify(shortLinkService, never()).resolveCode(code);
        verify(shortLinkService).lookupTargetWithoutCounting(code);
    }

    /* "nF" is 2 characters, below CODE_MIN_LENGTH, so the URL pattern rejects it
   during routing and the controller never runs.
   This test guards the HEAD mapping against drifting to a looser path pattern
   than the GET one: if it were declared as "/{code}" instead of the shared
   pattern constant, HEAD would accept codes that GET rejects and the request
   would reach the service.
   */
    @Test
    void head_withACodeThatDoesNotMatchThePattern_returns404WithoutReachingTheService() throws Exception {

        String code = "nF";

        mockMvc.perform(head("/{code}", code)).andExpect(status().isNotFound());

        verifyNoInteractions(shortLinkService);
    }
}
