package com.m4r10kyou.shortlinkQr.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rejects an alias that would collide with the first segment of a mapped
 * route. Such a link would never resolve: the route wins, and the caller
 * gets whatever that route does instead of the redirect.
 */
@Documented
@Target({ ElementType.FIELD , ElementType.ANNOTATION_TYPE , ElementType.METHOD , ElementType.CONSTRUCTOR, ElementType.PARAMETER , ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotReservedAliasValidator.class)
public @interface NotReservedAlias {

    String message() default "Custom Alias provided is reserved, choose another one";
    Class<?>[] groups() default{};
    Class<? extends Payload>[] payload() default{};
}
