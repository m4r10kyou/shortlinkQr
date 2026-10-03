package com.m4r10kyou.shortlinkQr.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.m4r10kyou.shortlinkQr.exception.QrGenerationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class QrCodeGenerator {

    private static final ErrorCorrectionLevel ERROR_CORRECTION = ErrorCorrectionLevel.H;

    //Spec minimum. Scanners need this quiet zone to find the symbol; a smaller margin breaks them
    private static final int MARGIN = 4;

    //PNG is mandatory. JPEG compression blurs the edges between modules, which breaks scanning
    private static final String FORMAT = "PNG";

    private final int size;
    private final Map<EncodeHintType, Object> hints;


    public QrCodeGenerator(@Value("${shortlink.qr.size:512}") int size) {

        this.size = size;
        this.hints = Map.of(
                EncodeHintType.ERROR_CORRECTION, ERROR_CORRECTION ,
                EncodeHintType.MARGIN, MARGIN ,
                EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name()
                );
    }

    /**
     * Renders the given URL as a PNG QR code.
     *
     * @throws QrGenerationException if the symbol cannot be encoded or written
     */
    public byte[] generate(String url){

        /*
         *   No try-with-resources because yteArrayOutputStream writes to an in-memory array,
         *   not a System resource,so close() does nothing
         */
        try {

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, FORMAT, outputStream);

            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new QrGenerationException("Cannot generate QR for the url: " + url, e);
        }
    }
}
