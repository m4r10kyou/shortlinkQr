package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.exception.InvalidAdminTokenException;
import com.m4r10kyou.shortlinkQr.service.AdminTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class HealthControllerTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private AdminTokenVerifier adminTokenVerifier;

    @Test
    void ping_returns200WithOkStatus() throws Exception {

        mockMvc.perform(get("/api/health/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void check_withoutHeader_returns200WithoutStatus() throws Exception {

        mockMvc.perform(get("/api/health/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project").value("shortlinkQr"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    void check_withValidHeader_returns200WithStatus() throws Exception {

        String token = "V4l1dTok3n";

        when(adminTokenVerifier.verify(eq(token)))
                .thenReturn(true);

        mockMvc.perform(get("/api/health/")
                        .header("X-Admin-Token",token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project").value("shortlinkQr"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void check_withInvalidHeader_returns403() throws Exception {

        String token = "inv4l1dTok3n";

        when(adminTokenVerifier.verify(eq(token)))
                .thenThrow(new InvalidAdminTokenException("Token provided for check health is invalid"));

        mockMvc.perform(get("/api/health/")
                        .header("X-Admin-Token",token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type")
                    .value("urn:problem-type:invalid-admin-token"));
    }

}
