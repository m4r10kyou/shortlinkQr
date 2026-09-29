package com.m4r10kyou.shortlinkQr.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.net.URISyntaxException;

public class HttpUrlValidator implements ConstraintValidator<HttpUrl,String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if(null == value){
            return true;
        }

        try{

            URI uri = new URI(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            return null != host
                    &&  null != scheme
                    &&  ( scheme.equalsIgnoreCase("http")
                        || scheme.equalsIgnoreCase("https")
                        );
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
