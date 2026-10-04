package com.m4r10kyou.shortlinkQr.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.ReaderException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

class QrLogoCoverageExperiment {

    private final QrCodeGenerator qrCodeGenerator = new QrCodeGenerator(512);

    @Test
    @Disabled("Manual experiment: Remove @Disabled to measure logo coverage. Do not run in CI.")
    void decode_withIncreasingLogoCoverage_printsSuccessRates() throws Exception{

        String baseUrl = "http://short.test/";
        List<String> sampleCodes = List.of(
                "abc1234", "xy9Z8q1", "A1b2C3d", "zXyWvUt", "9876543",
                "mNoPqRs", "1a2B3c4", "QWeRtYu", "pLmMkNj", "0o9I8u7"
        );

        System.out.println("=== QR Logo Coverage Experiment ===");
        System.out.println("% Ancho | % Área Aprox | Éxitos (sobre " + sampleCodes.size() + ") | Fallos");
        System.out.println("----------------------------------------------------------------------");

        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
        for( int percentage = 30; percentage <= 45; percentage += 1){

            int successes = 0;
            List<String> failedCodes = new ArrayList<>();

            for(String code : sampleCodes){

                String targetUrl = baseUrl + code;

                byte[] qrBytes = qrCodeGenerator.generate(targetUrl);
                BufferedImage qrImg = ImageIO.read( new ByteArrayInputStream(qrBytes));

                int imageWidth = qrImg.getWidth();
                int imageHeight = qrImg.getHeight();

                int plaqueSide = (int) (imageWidth * (percentage / 100.0));
                int x = (imageWidth - plaqueSide) / 2;
                int y = (imageHeight - plaqueSide) / 2;

                Graphics2D g = qrImg.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(x, y, plaqueSide, plaqueSide);
                g.dispose();

                BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(qrImg);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                try {
                    new QRCodeReader().decode(bitmap, hints);
                    successes++;
                } catch (ReaderException e) {
                    failedCodes.add(code);
                }
            }
            double areaCoverage = Math.pow(percentage / 100.0, 2) * 100;
            System.out.printf("   %02d%%   |     %02.0f%%     |      %d/10      | %s%n", percentage, areaCoverage, successes, failedCodes);
        }

        System.out.println("-----------------------------------");
        System.out.println("Note: The percentage is calculated over the total image (including quiet zone).");
        System.out.println("Therefore, the covered area relative to the actual symbol is slightly higher.");
    }
}
