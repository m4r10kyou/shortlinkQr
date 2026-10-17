package com.m4r10kyou.shortlinkQr.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManagementTokenGeneratorTest {

    private final ManagementTokenGenerator managementTokenGenerator = new ManagementTokenGenerator();

    @Test
    void generate_calledTwice_returnsDifferentRawTokens() {

        ManagementTokenGenerator.Token firstToken = managementTokenGenerator.generate();
        ManagementTokenGenerator.Token secondToken = managementTokenGenerator.generate();


        assertThat(firstToken.rawToken()).isNotEqualTo(secondToken.rawToken());
    }

    @Test
    void generate_returnsATokenWhoseHashMatchesTheHashMethod() {

        ManagementTokenGenerator.Token tokenGenerated = managementTokenGenerator.generate();

        String calculatedHash = managementTokenGenerator.hash(tokenGenerated.rawToken());

        assertThat(calculatedHash).isEqualTo(tokenGenerated.tokenHash());
    }

    @Test
    void hash_withTheSameInput_returnsTheSameOutput() {

        String input = "test-token-determinism";

        String hash1 = managementTokenGenerator.hash(input);
        String hash2 = managementTokenGenerator.hash(input);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void hash_withDifferentInputs_returnsDifferentOutputs() {

        String firstInput = "test-token-one";
        String secondInput = "test-token-two";

        String outputFirstInput = managementTokenGenerator.hash(firstInput);
        String outputSecondInput = managementTokenGenerator.hash(secondInput);

        assertThat(outputFirstInput).isNotEqualTo(outputSecondInput);

    }

    @Test
    void hash_returnsSixtyFourHexCharacters() {

        String hash = managementTokenGenerator.hash("test-token-regex");

        assertThat(hash).matches("^[0-9a-f]{64}$");
    }

    @Test
    void hash_withNull_throwsNullPointerException() {

        assertThatThrownBy(() -> managementTokenGenerator.hash(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void generate_returnsAFortyThreeCharacterRawToken() {

        String rawToken = managementTokenGenerator.generate().rawToken();

        assertThat(rawToken).hasSize(43);
    }

    @Test
    void hash_matchesTheKnownSha256Vector() {

        String rawToken = "ShaVector256Test";
        String hashToken = "1afe794031ddb20ccfd6b5764cfb9de14930c7bb513f82271eab6891ab3ba4bd";

        String actualHash = managementTokenGenerator.hash(rawToken);

        assertThat(actualHash).isEqualTo(hashToken);
    }
}
