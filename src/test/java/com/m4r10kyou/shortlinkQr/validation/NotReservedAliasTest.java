package com.m4r10kyou.shortlinkQr.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NotReservedAliasTest {

    private final NotReservedAliasValidator notReservedAliasValidator = new NotReservedAliasValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "cheetos",
            "hobbit"
    })
    void isValid_withCustomAliasAllowed_returnsTrue(String customAlias) {

        assertThat(notReservedAliasValidator.isValid(customAlias, null)).isTrue();
    }

    @Test
    void isValid_withNull_returnsTrue() {

        assertThat(notReservedAliasValidator.isValid(null, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "FiFa",
            "FIB"
    })
    void isValid_withCapsCharsAllowed_returnsTrue(String customAlias) {

        assertThat(notReservedAliasValidator.isValid(customAlias, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "api",
            "error",
            "h2-console",
            "actuator"
    })
    void isValid_withNotAllowedAlias_returnsFalse(String customAlias){

        assertThat(notReservedAliasValidator.isValid(customAlias, null)).isFalse();

    }

    @ParameterizedTest
    @ValueSource(strings = { "API", "Api", "ERROR", "H2-Console" })
    void isValid_withNotAllowedAliasInDifferentCase_returnsFalse(String customAlias) {

        assertThat(notReservedAliasValidator.isValid(customAlias, null)).isFalse();

    }
}
