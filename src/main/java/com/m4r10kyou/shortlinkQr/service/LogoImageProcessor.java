package com.m4r10kyou.shortlinkQr.service;

import com.m4r10kyou.shortlinkQr.exception.InvalidLogoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Component
public class LogoImageProcessor {

    private static final String FORMAT = "PNG";

    private final int maxBytes;
    private final int minDimension;
    private final int maxDimension;

    public LogoImageProcessor(@Value("${shortlink.logo.max-bytes:1048576}") int maxBytes,
                              @Value("${shortlink.logo.min-dimension:32}") int minDimension,
                              @Value("${shortlink.logo.max-dimension:2000}") int maxDimension
    ){

        this.maxBytes = maxBytes;
        this.minDimension = minDimension;
        this.maxDimension = maxDimension;
    }

    public byte[] sanitize(byte[] rawBytes) {
        
        ImageReader reader = null;

        if( null == rawBytes){
            throw new InvalidLogoException("Attached logo cannot be null");
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(rawBytes);
             ImageInputStream iis = ImageIO.createImageInputStream(bais)
             ) {

            if (null == iis) {

                throw new InvalidLogoException("No SPI is available to handle this attached logo");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (!readers.hasNext()) {
                throw new InvalidLogoException("Unsupported image format");
            }

            reader = readers.next();
            reader.setInput(iis, false);

            int width = reader.getWidth(0);
            int height = reader.getHeight(0);

            if (width < minDimension || width > maxDimension ||
                    height < minDimension || height > maxDimension) {

                throw new InvalidLogoException("Invalid image dimensions");
            }

            BufferedImage bufferedImage = reader.read(0);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            boolean writtenFlag = ImageIO.write(bufferedImage, FORMAT, baos);

            if (!writtenFlag) {

                throw new InvalidLogoException("Unable to convert image to PNG");
            }

            byte[] outputBytes = baos.toByteArray();

            if (outputBytes.length > maxBytes) {

                throw new InvalidLogoException("Converted image exceeds the maximum file size");
            }

            return outputBytes;

        } catch (IOException e) {

            throw new InvalidLogoException("Cannot process attached logo", e);
        } finally {

            if (reader != null) {
                reader.dispose();
            }
        }

    }
}
