package com.m4r10kyou.shortlinkQr.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ExpirationPolicyTest {

    @Test
    void isExpired_withNullExpiry_returnsFalse(){

        assertThat(ExpirationPolicy.isExpired(null)).isFalse();
    }

    @Test
    void isExpired_withAPastInstant_returnsTrue(){

        Instant time = Instant.now().minus(1 , ChronoUnit.HOURS);

        assertThat(ExpirationPolicy.isExpired(time)).isTrue();
    }

    @Test
    void isExpired_withAFutureInstant_returnsFalse(){

        Instant time = Instant.now().plus(5,ChronoUnit.DAYS);

        assertThat(ExpirationPolicy.isExpired(time)).isFalse();
    }
}
