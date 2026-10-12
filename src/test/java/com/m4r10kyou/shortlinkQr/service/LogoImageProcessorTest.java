package com.m4r10kyou.shortlinkQr.service;

import com.m4r10kyou.shortlinkQr.exception.InvalidLogoException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogoImageProcessorTest {

    private static final byte[] PNG_SIGNATURE = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 , 0x0D , 0x0A , 0x1A , 0x0A  };
    private static final String TEST_PNG = "/reicon--confetti-duotone.png";

    private final LogoImageProcessor imgProcessor = new LogoImageProcessor(1048576, 32,2000);

    @Test
    void sanitize_withAValidPng_returnsPngBytes() throws Exception  {

        byte[] inputBytes = loadFixture(TEST_PNG);
        BufferedImage inputImage = ImageIO.read(new ByteArrayInputStream(inputBytes));

        byte[] resultBytes = imgProcessor.sanitize(inputBytes);

        BufferedImage decodedResult = ImageIO.read(new ByteArrayInputStream(resultBytes));

        assertThat(decodedResult).isNotNull();
        assertThat(decodedResult.getWidth()).isEqualTo(inputImage.getWidth());
        assertThat(decodedResult.getHeight()).isEqualTo(inputImage.getHeight());

        assertThat(resultBytes).startsWith(PNG_SIGNATURE);
    }

    @Test
    void sanitize_withAJpeg_returnsPngBytes() throws Exception{

        // Dimensions >  32px
        int width = 64,height = 64;
        byte[] jpgBytes = generateImageFile(width,height);

        byte[] resultBytes = imgProcessor.sanitize(jpgBytes);

        BufferedImage decodedResult = ImageIO.read(new ByteArrayInputStream(resultBytes));

        assertThat(decodedResult).isNotNull();
        assertThat(decodedResult.getWidth()).isEqualTo(width);
        assertThat(decodedResult.getHeight()).isEqualTo(height);

        assertThat(resultBytes).startsWith(PNG_SIGNATURE);
    }

    @Test
    void sanitize_withBytesThatAreNotAnImage_throwsInvalidLogoException(){

        String errorMsg = "Unsupported image format";
        String noImage="That is a no image file";

        assertThatThrownBy(() -> imgProcessor.sanitize(noImage.getBytes()))
                .isInstanceOf(InvalidLogoException.class)
                .hasMessage(errorMsg);
    }

    @Test
    void sanitize_withATruncatedPng_throwsInvalidLogoException() throws Exception{

        //Retail length to truncated png file
        byte[] inputBytes = loadFixture(TEST_PNG);
        byte[] truncatedPng = Arrays.copyOf(inputBytes, (inputBytes.length)/2);

        assertThatThrownBy(() -> imgProcessor.sanitize(truncatedPng))
                .isInstanceOf(InvalidLogoException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void sanitize_withAnImageTallerThanTheMaximum_throwsInvalidLogoException() throws Exception{

        String errorMsg = "Invalid image dimensions";
        byte[] jpegBytes = generateImageFile(32,5000);

        assertThatThrownBy(() -> imgProcessor.sanitize(jpegBytes))
                .isInstanceOf(InvalidLogoException.class)
                .hasMessage(errorMsg);

    }

    @Test
    void sanitize_withAnImageSmallerThanTheMinimum_throwsInvalidLogoException() throws Exception{

        String errorMsg = "Invalid image dimensions";
        byte[] jpegBytes = generateImageFile(5,32);

        assertThatThrownBy(() -> imgProcessor.sanitize(jpegBytes))
                .isInstanceOf(InvalidLogoException.class)
                .hasMessage(errorMsg);

    }

    @Test
    void sanitize_withNullBytes_throwsInvalidLogoException() {

        String errorMsg = "Attached logo cannot be null";

        assertThatThrownBy(() -> imgProcessor.sanitize(null))
                .isInstanceOf(InvalidLogoException.class)
                .hasMessage(errorMsg);
    }

    @Test
    void sanitize_withAnImageExceedsMaximumFileSize_throwsInvalidLogoException() throws Exception{

        String errorMsg = "Converted image exceeds the maximum file size";
        LogoImageProcessor imgProcessorRidiculous = new LogoImageProcessor(10, 32,2000);
        byte[] jpegBytes = generateImageFile(32,32);

        assertThatThrownBy(() -> imgProcessorRidiculous.sanitize(jpegBytes))
                .isInstanceOf(InvalidLogoException.class)
                .hasMessage(errorMsg);

    }

    @Test
    void sanitize_withAPolyglotImage_removesAppendedGarbage() throws Exception {

        byte[] validPng = loadFixture(TEST_PNG);
        String maliciousPayload = "<?php system($_GET['cmd']); ?>";

        ByteArrayOutputStream polyglot = new ByteArrayOutputStream();
        polyglot.write(validPng);
        polyglot.write(maliciousPayload.getBytes());
        byte[] polyglotBytes = polyglot.toByteArray();

        byte[] resultBytes = imgProcessor.sanitize(polyglotBytes);

        assertThat(resultBytes).startsWith(PNG_SIGNATURE);
        assertThat(new String(resultBytes)).doesNotContain(maliciousPayload);
    }

    private byte[] loadFixture(String name) throws Exception  {

        try (InputStream img = getClass().getResourceAsStream(name)) {
            Objects.requireNonNull(img, "Fixture not found in classpath: " + name);
            return img.readAllBytes();
        }
    }

    // JPEG Fixture in memory TYPE_INT_RGB (without Alfa)
    private byte[] generateImageFile(int width, int height) throws Exception {

        BufferedImage jpegImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(jpegImage, "JPG", baos);
        return  baos.toByteArray();
    }
}
