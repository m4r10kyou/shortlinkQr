package com.m4r10kyou.shortlinkQr.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class QrCodeGeneratorTest {

    private final QrCodeGenerator generator = new QrCodeGenerator(512);

    @Test
    void generate_withAUrl_decodesBackToTheOriginalUrl() throws Exception {

        String originalUrl = "https://example.com/roundtrip-test";

        byte[] qrBytes = generator.generate(originalUrl);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(qrBytes));
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Result result = new QRCodeReader().decode(bitmap);

        assertThat(result.getText()).isEqualTo(originalUrl);
    }

    @Test
    void generate_withTheSameUrlTwice_returnsIdenticalBytes(){

        String originalUrl = "https://example.com/deterministic-test";

        byte[] firstRound = generator.generate(originalUrl);
        byte[] secondRound = generator.generate(originalUrl);

        assertThat(firstRound).isEqualTo(secondRound);
    }
}
