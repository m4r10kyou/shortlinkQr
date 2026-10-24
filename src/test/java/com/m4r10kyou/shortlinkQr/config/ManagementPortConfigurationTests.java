package com.m4r10kyou.shortlinkQr.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Configuration guard: fails if management.server.port is deleted, misspelled or
 * set to a value that collides with server.port in the prod profile.
 *
 * It loads the committed application.properties and application-prod.properties
 * through Spring Boot's own config-data machinery
 * ({@link ConfigDataEnvironmentPostProcessor#applyTo}), so profile merging and
 * placeholder resolution behave exactly as in the real application; this test
 * re-implements none of that logic. The only controlled part is the OS
 * environment, replaced by a test-scoped source.
 *
 * Scenarios: PORT unset (local default branch of server.port=${PORT:9080}) and
 * PORT=10000, an arbitrary stand-in for "the platform assigns some port".
 * It asserts nothing about the port of any actual deployment, which the hosting
 * platform assigns at runtime and no committed test can observe.
 */
class ManagementPortConfigurationTests {

    private static final String MANAGEMENT_PORT_KEY = "management.server.port";
    private static final String SERVER_PORT_KEY = "server.port";

    @Test
    void managementServerPort_inCheckedProdEnvironments_staysConfiguredAndDistinctFromServerPort() {
        assertPortsDoNotCollide(Map.of());
        assertPortsDoNotCollide(Map.of("PORT", "10000"));
    }

    private void assertPortsDoNotCollide(Map<String, Object> systemEnvironment) {
        MockEnvironment environment = loadCommittedConfigWithProdProfile(systemEnvironment);

        String managementPortValue = environment.getProperty(MANAGEMENT_PORT_KEY);
        assertThat(managementPortValue)
                .as("management.server.port must stay configured in the prod profile "
                        + "so Actuator is isolated from the public port (environment=%s)", systemEnvironment)
                .isNotBlank();

        int managementPort = Integer.parseInt(managementPortValue);
        int serverPort = Integer.parseInt(environment.getProperty(SERVER_PORT_KEY, "8080"));

        assertThat(managementPort)
                .as("management.server.port must be a valid TCP port")
                .isBetween(1, 65535);
        assertThat(managementPort)
                .as("management.server.port must differ from server.port (environment=%s)", systemEnvironment)
                .isNotEqualTo(serverPort);
    }

    private MockEnvironment loadCommittedConfigWithProdProfile(Map<String, Object> systemEnvironment) {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(
                new SystemEnvironmentPropertySource("controlledSystemEnvironment", systemEnvironment));
        environment.getPropertySources().addFirst(
                new MapPropertySource("testProfiles", Map.of("spring.profiles.active", "prod")));

        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        return environment;
    }

}
