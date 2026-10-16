package com.m4r10kyou.shortlinkQr.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShortLinkTest {

    private final ShortLink shortLink = new ShortLink("mi-repo", "https://example.com",null,"a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2");
    private final byte[] dummyImage = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 };
    private final String hashToken = "1afe794031ddb20ccfd6b5764cfb9de14930c7bb513f82271eab6891ab3ba4bd";


    @Test
    void attachLogo_withBytes_storesTheLogo() {

        shortLink.attachLogo(dummyImage);

        assertThat(shortLink.hasLogo()).isTrue();

        assertThat(shortLink.getLogoData()).isEqualTo(dummyImage);
    }

    @Test
    void attachLogo_withNull_throwsNullPointerException(){

        String errorMsg ="Logo cannot be null";

        assertThatThrownBy(() -> shortLink.attachLogo(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(errorMsg);
    }

    @Test
    void attachLogo_withEmptyArray_throwsIllegalArgumentException(){

        String errorMsg ="Logo array cannot be empty";

        assertThatThrownBy(() -> shortLink.attachLogo(new byte[] {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(errorMsg);
    }

    @Test
    void getLogoData_withoutLogo_returnsNull(){

        assertThat(shortLink.getLogoData()).isNull();
    }

    @Test
    void removeLogo_withALogo_clearsIt(){

        shortLink.attachLogo(dummyImage);

        assertThat(shortLink.hasLogo()).isTrue();

        shortLink.removeLogo();

        assertThat(shortLink.hasLogo()).isFalse();
    }

    @Test
    void attachLogo_whenTheCallerMutatesItsArrayAfterwards_keepsTheStoredBytes(){

        shortLink.attachLogo(dummyImage);

        dummyImage[0]=0;

        assertThat(shortLink.getLogoData()).isEqualTo(new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 });
    }

    @Test
    void getLogoData_whenTheCallerMutatesTheReturnedArray_keepsTheStoredBytes(){

        shortLink.attachLogo(dummyImage);

        byte[] attachedLogo = shortLink.getLogoData();

        attachedLogo[0]=0;

        assertThat(shortLink.getLogoData()).isEqualTo(new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 });
    }

    @Test
    void matchesManagementToken_withTheStoredHash_returnsTrue(){

        ShortLink shortLink = new ShortLink("storedHash", "https://example.com",null,hashToken);

        assertThat(shortLink.matchesManagementToken(hashToken)).isTrue();
    }

    @Test
    void matchesManagementToken_withAnotherHash_returnsFalse(){

        String anotherHashToken = "n0v4l1dh4sht0k3nn0v4l1dh4sht0k3nn0v4l1dh4sht0k3nn0v4l1dh4sht0k3n";
        String hashDifferingInTheLastCharacter = "1afe794031ddb20ccfd6b5764cfb9de14930c7bb513f82271eab6891ab3ba4be";

        ShortLink shortLink = new ShortLink("no-matched-hash", "https://example.com",null,hashToken);

        assertThat(shortLink.matchesManagementToken(anotherHashToken)).isFalse();
        assertThat(shortLink.matchesManagementToken(hashDifferingInTheLastCharacter)).isFalse();
    }

    @Test
    void matchesManagementToken_withNull_returnsFalse(){

        ShortLink shortLink = new ShortLink("token-null", "https://example.com",null,hashToken);

        assertThat(shortLink.matchesManagementToken(null)).isFalse();
    }

    @Test
    void matchesManagementToken_withBlank_returnsFalse(){

        ShortLink shortLink = new ShortLink("blank-token", "https://example.com",null,hashToken);

        assertThat(shortLink.matchesManagementToken("   ")).isFalse();
    }

    @Test
    void matchesManagementToken_whenNoHashIsStored_returnsFalse(){

        ShortLink shortLink = new ShortLink("no-token-stored", "https://example.com",null,null);

        assertThat(shortLink.matchesManagementToken(hashToken)).isFalse();
    }

    @Test
    void changeExpiry_withNull_throwsNullPointerException(){

        String errorMsg ="Expiration cannot be null";

        assertThatThrownBy(() -> shortLink.changeExpiry(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(errorMsg);
    }

    @Test
    void changeTargetUrl_withNull_throwsNullPointerException(){

        String errorMsg ="Target url cannot be null";

        assertThatThrownBy(() -> shortLink.changeTargetUrl(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(errorMsg);
    }
}
