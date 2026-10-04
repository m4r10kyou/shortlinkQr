package com.m4r10kyou.shortlinkQr.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class CodeGeneratorTest {

    private final CodeGenerator generator = new CodeGenerator(3);

    @Test
    void generate_withNoCollision_returnsCodeOfFixedLength() {

        String code = generator.generate(c -> false);
        assertThat(code).hasSize(7);
    }

    @Test
    void generate_withNoCollision_returnsOnlyBase62Characters() {

        String code = generator.generate(c -> false);
        assertThat(code).matches("[a-zA-Z0-9]+");

    }

    @Test
    void generate_withCollisions_returnsAnAvailableCode() {

        AtomicInteger attempts = new AtomicInteger(0);
        String code = generator.generate(c -> attempts.getAndIncrement() < 2);

        assertThat(code).hasSize(7);
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void generate_whenRetriesAreExhausted_throwsIllegalState() {

        assertThatThrownBy(() -> generator.generate(c -> true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot generate code after 3 attempts.");
    }
}
