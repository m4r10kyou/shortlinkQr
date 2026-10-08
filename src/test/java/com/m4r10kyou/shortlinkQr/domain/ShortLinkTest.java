package com.m4r10kyou.shortlinkQr.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShortLinkTest {

    private final ShortLink shortLink = new ShortLink("mi-repo", "https://example.com",null);
    private final byte[] dummyImage = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 };

    @Test
    void attachLogo_withBytes_storesTheLogo() {

        shortLink.attachLogo(dummyImage);

        assertThat(shortLink.hasLogo()).isTrue();

        assertThat(shortLink.getLogoData()).isEqualTo(dummyImage);
    }

    @Test
    void attachLogo_withNull_throwsNullPointerException(){

        assertThatThrownBy(() -> shortLink.attachLogo(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void attachLogo_withEmptyArray_throwsIllegalArgumentException(){

        assertThatThrownBy(() -> shortLink.attachLogo(new byte[] {}))
                .isInstanceOf(IllegalArgumentException.class);
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

}
