package com.m4r10kyou.shortlinkQr.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.m4r10kyou.shortlinkQr.exception.InvalidLogoException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrCodeGeneratorTest {

    private final QrCodeGenerator generator = new QrCodeGenerator(512);

    @Test
    void generate_withAUrl_decodesBackToTheOriginalUrl() throws Exception {

        String originalUrl = "https://example.com/roundtrip-test";

        byte[] qrBytes = generator.generate(originalUrl);

        assertThat(decodeText(qrBytes)).isEqualTo(originalUrl);
    }

    @Test
    void generate_withTheSameUrlTwice_returnsIdenticalBytes(){

        String originalUrl = "https://example.com/deterministic-test";

        byte[] firstRound = generator.generate(originalUrl);
        byte[] secondRound = generator.generate(originalUrl);

        assertThat(firstRound).isEqualTo(secondRound);
    }

    @Test
    void generateWithLogo_withAPngLogo_decodesBackToTheOriginalUrl() throws Exception {

        String originalUrl = "https://example.com/test_for_png_qr";
        byte[] logoBytes = loadFixture("/reicon--confetti-duotone.png");

        byte[] qrBytes = generator.generateWithLogo(originalUrl, logoBytes);

        assertThat(decodeText(qrBytes)).isEqualTo(originalUrl);
    }

    // REGRESSION TEST:
    // MatrixToImageWriter.toBufferedImage returns a TYPE_BYTE_BINARY image: one bit per
    // pixel, two colors, black and white. Drawing a color logo onto that canvas silently
    // quantizes every pixel to one of the two, so a dark red seal came out solid black and
    // a blue icon came out as a white silhouette. The fix is to copy the symbol onto a
    // TYPE_INT_RGB canvas before drawing. Any pixel that is neither pure black nor pure
    // white can only come from the logo, which is why this scans the whole image.
    @Test
    void generateWithLogo_withAColorLogo_keepsTheLogoInColor() throws Exception {

        String originalUrl = "https://example.com/test_for_png_qr";
        byte[] logoBytes = loadFixture("/reicon--confetti-duotone.png");

        byte[] qrBytes = generator.generateWithLogo(originalUrl, logoBytes);

        BufferedImage qrImage = ImageIO.read(new ByteArrayInputStream(qrBytes));

        assertThat(hasColorPixel(qrImage))
                .as("the generated QR has no color pixel: the logo was quantized to black and white")
                .isTrue();
    }

    @Test
    void generateWithLogo_withAPngLogo_returnsDifferentBytesThanPlainQr() throws Exception{

        String originalUrl = "https://example.com/test_for_png_qr";
        byte[] logoBytes = loadFixture("/reicon--confetti-duotone.png");

        byte[] simpleQr = generator.generate(originalUrl);
        byte[] logoQr = generator.generateWithLogo(originalUrl, logoBytes);

        assertThat(simpleQr).isNotEqualTo(logoQr);
    }

    @Test
    void generateWithLogo_withBytesThatAreNotAnImage_throwsInvalidLogoException() {

        String originalUrl = "https://example.com/test_for_png_qr";
        byte[] noLogoBytes = new byte[10];
        String errorMsg = "Attached logo bytes cannot be decoded as an image";

        assertThatThrownBy(() -> generator.generateWithLogo(originalUrl, noLogoBytes))
                .isInstanceOf(InvalidLogoException.class)
                .hasMessage(errorMsg);
    }

    @Test
    void generateWithLogo_withTheSameUrlAndLogoTwice_returnsIdenticalBytes() throws Exception {

        String originalUrl = "https://example.com/test_for_png_qr";
        byte[] logoBytes = loadFixture("/reicon--confetti-duotone.png");

        byte[] firstQrBytes = generator.generateWithLogo(originalUrl, logoBytes);
        byte[] secondQrBytes = generator.generateWithLogo(originalUrl, logoBytes);

        assertThat(firstQrBytes).isEqualTo(secondQrBytes);
    }


    private byte[] loadFixture(String name) throws Exception  {

        try (InputStream img = getClass().getResourceAsStream(name)) {
            Objects.requireNonNull(img, "Fixture not found in classpath: " + name);
            return img.readAllBytes();
        }
    }

    private String decodeText(byte[] pngBytes) throws Exception {

        BufferedImage qrImage = ImageIO.read(new ByteArrayInputStream(pngBytes));

        LuminanceSource source = new BufferedImageLuminanceSource(qrImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        // PURE_BARCODE tells ZXing the image is a generated barcode with nothing
        // around it, so it samples the module grid directly instead of locating the
        // finder patterns and resampling through a perspective transform. Without
        // the hint, one short code in ten failed to decode from an undamaged PNG:
        // a sample point lands on a module boundary and reads the wrong bit. See
        // QrLogoCoverageExperiment, where this was found.
        Map<DecodeHintType, Object> hints = Map.of(DecodeHintType.PURE_BARCODE, Boolean.TRUE);

        Result result = new QRCodeReader().decode(bitmap, hints);

        return result.getText();
    }

    private boolean hasColorPixel(BufferedImage qrImage){

        for(int x = 0 ; qrImage.getWidth() > x; x++){

            for(int y = 0 ; qrImage.getHeight() > y; y++){

                int color = qrImage.getRGB(x,y);

                if(Color.WHITE.getRGB() != color  && Color.BLACK.getRGB() != color  ){

                    return true;
                }
            }
        }
        return false;
    }
}
