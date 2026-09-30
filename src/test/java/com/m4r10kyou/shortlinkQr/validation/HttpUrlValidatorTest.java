package com.m4r10kyou.shortlinkQr.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;


public class HttpUrlValidatorTest {

    private HttpUrlValidator  httpUrlValidator;

    @BeforeEach
    public void setUp() {

        this.httpUrlValidator = new HttpUrlValidator();
    }

    /*
     * NOTE ABOUT THE CONTEXT:
     * We pass 'null' as the ConstraintValidatorContext in all tests. This works because
     * the current implementation of HttpUrlValidator does not use the context to build messages.
     * If custom dynamic error messages are added in the future, this 'null'
     * will throw a NullPointerException, and the context will need to be instantiated or mocked.
     */

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com",
            "http://example.com/menu?season=1&dish=2"
    })
    void acceptsValidUrls(String url) {
        assertThat(httpUrlValidator.isValid(url, null)).isTrue();
    }

    @Test
    void acceptsNullToRespectBeanValidationContract() {

        assertThat(httpUrlValidator.isValid(null, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "mailto:someone@example.com",
            "ftp://files.example.com/x",
            "this-is-not-valid-url",
            ""
    })
    void rejectsInvalidUrls(String url) {

        assertThat(httpUrlValidator.isValid(url, null)).isFalse();
    }


    // REGRESSION TEST:
    // A space in the URL breaks the HTTP redirection by generating an invalid Location header.
    // This test documents the original bug and prevents anyone from accidentally loosening the syntax check.
    @Test
    void rejectsUrlWithSpaceThatWouldBreakTheRedirect() {

        String withSpaceUrl = "http://restaurant.example.com/menu summer.pdf";
        assertThat(httpUrlValidator.isValid(withSpaceUrl, null)).isFalse();
    }

    @Test
    void acceptsUrlWithPercentEncodedSpace() {

        String withSpaceUrl = "http://restaurant.example.com/menu%20summer.pdf";
        assertThat(httpUrlValidator.isValid(withSpaceUrl, null)).isTrue();;
    }
}
