package com.m4r10kyou.shortlinkQr.validation;

import com.m4r10kyou.shortlinkQr.exception.InvalidLogoException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogoUploadValidatorTest {

    private final LogoUploadValidator logoUploadValidator = new LogoUploadValidator(1048576);

    @Test
    void validate_withZeroBytes_throwsInvalidLogoException(){

        String errorMsg ="Attached logo is empty";

        assertThatThrownBy(() ->logoUploadValidator.validate(0,"image/png"))
                .isInstanceOf(InvalidLogoException.class)
                .hasMessage(errorMsg);
    }

    @Test
    void validate_withSizeExactlyAtTheLimit_doesNotThrow(){

        int maxUploadBytesAllowed = 100;
        int uploadBytes = 100;

        LogoUploadValidator uploadValidator = new LogoUploadValidator(maxUploadBytesAllowed);

        assertThatCode(() -> uploadValidator.validate(uploadBytes,"image/png"))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_withSizeOneByteOverTheLimit_throwsInvalidLogoException(){

        int maxUploadBytesAllowed = 100;
        int uploadBytes = 101;
        String errorMsg = "Attached logo exceeds the maximum file size";

        LogoUploadValidator uploadValidator = new LogoUploadValidator(maxUploadBytesAllowed);

        assertThatThrownBy(() ->uploadValidator.validate(uploadBytes,"image/png"))
                .isInstanceOf(InvalidLogoException.class)
                .hasMessage(errorMsg);
    }

    @Test
    void validate_withANullContentType_throwsInvalidLogoException(){

        String contentType = null;

        String errorMsg = "Missing or empty content type";

        assertThatThrownBy(() -> logoUploadValidator.validate(102,contentType))
                .isInstanceOf(InvalidLogoException.class)
                .hasMessage(errorMsg);
    }

    @Test
    void validate_withABlankContentType_throwsInvalidLogoException(){

        String contentType = "";

        String errorMsg = "Missing or empty content type";

        assertThatThrownBy(() -> logoUploadValidator.validate(219,contentType))
                .isInstanceOf(InvalidLogoException.class)
                .hasMessage(errorMsg);
    }


    @Test
    void validate_withANonImageContentType_throwsInvalidLogoException(){

        String contentType = "text/javascript";

        String errorMsg = "Attached logo has no allowed content type: "+contentType;

        assertThatThrownBy(() -> logoUploadValidator.validate(985,contentType))
                .isInstanceOf(InvalidLogoException.class)
                .hasMessage(errorMsg);
    }

    @Test
    void validate_withAnUppercaseContentType_doesNotThrow(){

        String contentType = "IMAGE/PNG";

        assertThatCode(() -> logoUploadValidator.validate(234,contentType))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_withAContentTypeCarryingParameters_doesNotThrow(){

        String contentType = "image/jpeg; charset=utf-8";

        assertThatCode(() -> logoUploadValidator.validate(345,contentType))
                .doesNotThrowAnyException();

    }

    @Test
    void validate_withAValidPngUpload_doesNotThrow()  {

        byte[] pngBytes = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 , 0x0D , 0x0A , 0x1A , 0x0A  };
        int size = pngBytes.length;
        String contentType = "image/png";

        assertThatCode(() -> logoUploadValidator.validate(size,contentType))
                .doesNotThrowAnyException();

    }

    @Test
    void allowedContentTypes_areAllReadableByImageIO(){

        List<String> supportedMimeTypes = Arrays.asList(ImageIO.getReaderMIMETypes());

        assertThat(supportedMimeTypes).containsAll(LogoUploadValidator.ALLOWED_CONTENT_TYPES);

    }
}
