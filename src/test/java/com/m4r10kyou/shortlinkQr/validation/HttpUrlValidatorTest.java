package com.m4r10kyou.shortlinkQr.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class HttpUrlValidatorTest {

    private final HttpUrlValidator httpUrlValidator = new HttpUrlValidator();

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
    void isValid_withHttpAndHttpsUrls_returnsTrue(String url) {

        assertThat(httpUrlValidator.isValid(url, null)).isTrue();
    }

    @Test
    void isValid_withNull_returnsTrue() {

        assertThat(httpUrlValidator.isValid(null, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "mailto:someone@example.com",
            "ftp://files.example.com/x"
    })
    void isValid_withUnsupportedScheme_returnsFalse(String url) {

        assertThat(httpUrlValidator.isValid(url, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "this-is-not-valid-url",
            ""
    })
    void isValid_withoutSchemeOrHost_returnsFalse(String url) {

        assertThat(httpUrlValidator.isValid(url, null)).isFalse();
    }


    // REGRESSION TEST:
    // A space in the URL breaks the HTTP redirection by generating an invalid Location header.
    // This test documents the original bug and prevents anyone from accidentally loosening the syntax check.
    @Test
    void isValid_withRawSpace_returnsFalse() {

        String withSpaceUrl = "http://restaurant.example.com/menu summer.pdf";
        assertThat(httpUrlValidator.isValid(withSpaceUrl, null)).isFalse();
    }

    @Test
    void isValid_withPercentEncodedSpace_returnsTrue() {

        String encodedSpaceUrl = "http://restaurant.example.com/menu%20summer.pdf";
        assertThat(httpUrlValidator.isValid(encodedSpaceUrl, null)).isTrue();
    }
}
