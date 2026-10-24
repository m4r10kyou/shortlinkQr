package com.m4r10kyou.shortlinkQr;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mechanism test: proves that Spring Boot serves Actuator on the management port
 * and not on the public port WHEN the separation properties are in place.
 *
 * It forces management.server.port=0 (random), so it intentionally does NOT read
 * the committed configuration: it keeps passing even if the real properties are
 * deleted.
 *
 * Every HTTP call carries explicit connect and read timeouts so a miswired
 * application fails fast instead of hanging the suite.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.server.port=0"
        })
@ActiveProfiles("test")
class ActuatorSeparationTests {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @LocalServerPort
    private int serverPort;

    @LocalManagementPort
    private int managementPort;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    @Test
    void actuatorHealth_onManagementPort_returnsUp() throws Exception {
        HttpResponse<String> response = get(managementPort);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void actuatorHealth_onServerPort_returnsNotFound() throws Exception {
        HttpResponse<String> response = get(serverPort);

        assertThat(response.statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> get(int port) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/actuator/health"))
                .timeout(READ_TIMEOUT)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

}
