package com.m4r10kyou.shortlinkQr.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.m4r10kyou.shortlinkQr.exception.InvalidLogoException;
import com.m4r10kyou.shortlinkQr.exception.QrGenerationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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

    // Measured, not chosen. QrLogoCoverageExperiment paints a white plate over the symbol at
    // increasing sizes and decodes the result: at 32% of the image width all ten sample codes
    // still decode, at 33% three of them fail, and from 38% none do. 25% is that ceiling minus
    // a margin, because the measurement is a best case - a lossless PNG decoded in memory -
    // while a real scan involves a camera, an angle, print quality and light.
    private static final double PLATE_TO_IMAGE_RATIO = 0.25;

    // Visual criterion, not a measurement. The logo fills this fraction of the plate, so a white
    // ring always separates it from the surrounding modules. A logo reaching the plate edge
    // would put its dark pixels against the dark modules and blur the boundary.
    private static final double LOGO_TO_PLATE_RATIO = 0.8;

    // Corner radius of the plate, as a divisor of its side. Visual criterion. Expressed as a
    // fraction of the side rather than in pixels so it scales with shortlink.qr.size.
    private static final int ARC_DIVISIONS = 5 ;

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

        try {

            BufferedImage image = renderSymbol(url);

            return toPngBytes(image);

        } catch (WriterException | IOException e) {
            throw new QrGenerationException("Cannot generate QR for the url: " + url, e);
        }
    }

    private BufferedImage renderSymbol(String url) throws WriterException {

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, size, size, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    /*
     *   No try-with-resources because ByteArrayOutputStream writes to an in-memory array,
     *   not a System resource,so close() does nothing
     */
    private byte [] toPngBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, FORMAT, outputStream);

        return outputStream.toByteArray();
    }

    public byte[] generateWithLogo(String originalUrl , byte[] logoBytes) {

        BufferedImage qrColorCanvas;
        BufferedImage attachedLogo;

        try {

            qrColorCanvas = toColorCanvas(renderSymbol(originalUrl));
            attachedLogo = ImageIO.read(new ByteArrayInputStream(logoBytes));

            if (attachedLogo == null) {
                throw new InvalidLogoException("Attached logo bytes cannot be decoded as an image");
            }

            int plateSide = (int) (qrColorCanvas.getWidth() * PLATE_TO_IMAGE_RATIO);
            int plateX = (qrColorCanvas.getWidth() - plateSide) /2 ;
            int plateY = (qrColorCanvas.getHeight() - plateSide) /2 ;

            double innerSquareSide = (plateSide * LOGO_TO_PLATE_RATIO);

            double scaleW =  innerSquareSide / attachedLogo.getWidth();
            double scaleH =  innerSquareSide / attachedLogo.getHeight();
            double finalScale = Math.min(scaleW, scaleH);

            int logoWidth = (int) Math.round(attachedLogo.getWidth() * finalScale) ;
            int logoHeight = (int) Math.round(attachedLogo.getHeight() * finalScale) ;

            int logoX = (plateX + (plateSide  - logoWidth) / 2);
            int logoY = (plateY + (plateSide  - logoHeight) / 2);

            Graphics2D graphics2D = qrColorCanvas.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            graphics2D.setColor(Color.WHITE);
            int arc = plateSide / ARC_DIVISIONS ;

            graphics2D.fillRoundRect(plateX, plateY, plateSide, plateSide, arc, arc);
            graphics2D.drawImage(attachedLogo, logoX , logoY, logoWidth, logoHeight, null);

            graphics2D.dispose();

            return toPngBytes(qrColorCanvas);
        } catch (WriterException | IOException e) {
            throw new QrGenerationException("Cannot generate QR for the url: " + originalUrl, e);
        }
    }

    /*
     * MatrixToImageWriter returns a TYPE_BYTE_BINARY canvas: one bit per pixel, two colors.
     * Painting a color logo directly onto it quantizes every pixel to black or white - a dark
     * red seal came out solid black, a blue icon came out as a white silhouette - and it also
     * flattens the antialiased edges of the plate. This explicit copy to TYPE_INT_RGB is what
     * makes the canvas type visible instead of inherited.
     */
    private BufferedImage toColorCanvas(BufferedImage symbol){

        BufferedImage colorCanvas = new BufferedImage(symbol.getWidth(), symbol.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = colorCanvas.createGraphics();
        graphics2D.drawImage(symbol, 0, 0, null);
        graphics2D.dispose();

        return colorCanvas;
    }
}
