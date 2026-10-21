package com.m4r10kyou.shortlinkQr.validation;

import com.m4r10kyou.shortlinkQr.domain.ShortLinkConstraints;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotReservedAliasValidator implements ConstraintValidator<NotReservedAlias,String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if(null == value){
            return true;
        }

        return ShortLinkConstraints.NOT_ALLOWED_ALIAS.stream()
                .noneMatch(value::equalsIgnoreCase);
    }
}
