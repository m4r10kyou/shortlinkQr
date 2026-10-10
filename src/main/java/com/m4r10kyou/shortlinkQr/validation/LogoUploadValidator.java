package com.m4r10kyou.shortlinkQr.validation;

import com.m4r10kyou.shortlinkQr.exception.InvalidLogoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class LogoUploadValidator {

    private final int maxUploadBytes;

    static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/png",
            "image/jpeg",
            "image/gif"
    );


    public LogoUploadValidator(@Value("${shortlink.logo.max-upload-bytes:1048576}") int maxUploadBytes) {
        this.maxUploadBytes = maxUploadBytes;
    }

    public void validate(long size, String contentType){

        if( 0 == size){

            throw new InvalidLogoException("Attached logo is empty");
        }

        if(maxUploadBytes < size){

            throw new InvalidLogoException("Attached logo exceeds the maximum file size");
        }

        if (contentType == null || contentType.isBlank()) {

            throw new InvalidLogoException("Missing or empty content type");
        }

        String mimeType = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);

        if(!ALLOWED_CONTENT_TYPES.contains(mimeType)){

            throw new InvalidLogoException("Attached logo has no allowed content type: "+mimeType);
        }
    }
}
