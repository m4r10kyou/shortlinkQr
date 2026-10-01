package com.m4r10kyou.shortlinkQr.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ ElementType.FIELD , ElementType.ANNOTATION_TYPE , ElementType.METHOD , ElementType.CONSTRUCTOR, ElementType.PARAMETER , ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HttpUrlValidator.class)
public @interface HttpUrl {

    String message() default "Target URL must be a valid HTTP or HTTPS URL";
    Class<?>[] groups() default{};
    Class<? extends Payload>[] payload() default{};
}
