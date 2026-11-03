package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitInterceptorTest {

    private static final int LIMIT = 2;
    private static final long WINDOW_SECONDS = 60;
    private static final long MAX_TRACKED_IPS = 1000;

    private final static String CONNECTING_IP = "CF-Connecting-IP";
    private static final String CLIENT_IP = "203.0.113.10";


    @Test
    void preHandle_whenTheLimitIsExceeded_throwsRateLimitExceededException() {

        RateLimitInterceptor rateLimitInterceptor = new RateLimitInterceptor(LIMIT, WINDOW_SECONDS, MAX_TRACKED_IPS);

        MockHttpServletRequest request = requestFrom("192.168.0.1", "POST", "/api/links");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(rateLimitInterceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(rateLimitInterceptor.preHandle(request, response, new Object())).isTrue();

        assertThatThrownBy(() -> rateLimitInterceptor.preHandle(request, response, new Object()))
                .isInstanceOfSatisfying(RateLimitExceededException.class,
                        ex -> assertThat(ex.getRetryAfterSeconds()).isEqualTo(WINDOW_SECONDS));
    }

    @Test
    void preHandle_withADifferentIp_countsSeparately() {

        RateLimitInterceptor rateLimitInterceptor = new RateLimitInterceptor(LIMIT, WINDOW_SECONDS, MAX_TRACKED_IPS);

        MockHttpServletRequest requestOne = requestFrom("192.168.0.1", "POST", "/api/links");
        MockHttpServletRequest requestTwo = requestFrom("192.168.10.1", "POST", "/api/links");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(rateLimitInterceptor.preHandle(requestOne, response, new Object())).isTrue();
        assertThat(rateLimitInterceptor.preHandle(requestTwo, response, new Object())).isTrue();

        assertThatNoException().isThrownBy(() -> rateLimitInterceptor.preHandle(requestOne, response, new Object()));
    }

    @Test
    void preHandle_withARequestOutOfScope_doesNotConsumeQuota() {

        RateLimitInterceptor rateLimitInterceptor = new RateLimitInterceptor(LIMIT, WINDOW_SECONDS, MAX_TRACKED_IPS);

        MockHttpServletRequest requestHealth = requestFrom("192.168.0.1", "GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(rateLimitInterceptor.preHandle(requestHealth, response, new Object())).isTrue();
        assertThat(rateLimitInterceptor.preHandle(requestHealth, response, new Object())).isTrue();
        assertThat(rateLimitInterceptor.preHandle(requestHealth, response, new Object())).isTrue();
        assertThat(rateLimitInterceptor.preHandle(requestHealth, response, new Object())).isTrue();

        MockHttpServletRequest requestApi = requestFrom("192.168.0.1", "POST", "/api/links");

        assertThatNoException().isThrownBy(() -> rateLimitInterceptor.preHandle(requestApi, response, new Object()));
    }

    /*
     * Regression test. In production getRemoteAddr() returns the Cloudflare edge
     * that happened to take the request, and the edge changes between requests -
     * four distinct ones appeared within a second and a half. Keying on it meant
     * one counter per edge, so the effective limit was several times the
     * configured one and unpredictable.
     */
    @Test
    void preHandle_withTheConnectingIpHeader_countsByItAndNotByThePeer() {

        RateLimitInterceptor rateLimitInterceptor = new RateLimitInterceptor(LIMIT, WINDOW_SECONDS, MAX_TRACKED_IPS);

        MockHttpServletRequest requestOne = requestFromWithConnectingIp("162.158.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(rateLimitInterceptor.preHandle(requestOne, response, new Object())).isTrue();
        assertThat(rateLimitInterceptor.preHandle(requestOne, response, new Object())).isTrue();

        assertThatThrownBy(() -> rateLimitInterceptor.preHandle(requestOne, response, new Object()))
                .isInstanceOfSatisfying(RateLimitExceededException.class,
                        ex -> assertThat(ex.getRetryAfterSeconds()).isEqualTo(WINDOW_SECONDS));

        MockHttpServletRequest requestTwo = requestFromWithConnectingIp("172.68.1.1");

        assertThatThrownBy(() -> rateLimitInterceptor.preHandle(requestTwo, response, new Object()))
                .isInstanceOfSatisfying(RateLimitExceededException.class,
                        ex -> assertThat(ex.getRetryAfterSeconds()).isEqualTo(WINDOW_SECONDS));
    }

    private MockHttpServletRequest requestFrom(String ip, String method, String uri) {

        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(ip);

        return request;
    }


    private MockHttpServletRequest requestFromWithConnectingIp(String ip) {

        MockHttpServletRequest request = requestFrom(ip, "POST", "/api/links");

        request.addHeader(CONNECTING_IP, CLIENT_IP);

        return request;
    }
}