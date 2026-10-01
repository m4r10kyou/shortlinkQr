package com.m4r10kyou.shortlinkQr.web;

import com.m4r10kyou.shortlinkQr.exception.AliasAlreadyExistsException;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkExpiredException;
import com.m4r10kyou.shortlinkQr.exception.ShortLinkNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ShortLinkNotFoundException.class)
    public ProblemDetail handleNotFound(ShortLinkNotFoundException ex) {

        log.debug("Short link not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );

        problem.setTitle("Short link not found");
        problem.setType(URI.create("urn:problem-type:not-found"));
        problem.setProperty("code", ex.getCode());

        return problem;
    }

    @ExceptionHandler(AliasAlreadyExistsException.class)
    public ProblemDetail handleExist(AliasAlreadyExistsException ex){

        log.debug("Alias already exists: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Alias already exists");
        problem.setType(URI.create("urn:problem-type:alias-occupied"));
        problem.setProperty("alias", ex.getAlias());

        return problem;
    }

    @ExceptionHandler(ShortLinkExpiredException.class)
    public ProblemDetail handleExpired(ShortLinkExpiredException ex){

        log.debug("Short link expired: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.GONE,
                ex.getMessage()
        );
        problem.setTitle("Short link expired");
        problem.setType(URI.create("urn:problem-type:link-expired"));
        problem.setProperty("code", ex.getCode());

        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ){

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> Objects.requireNonNullElse(
                                fieldError.getDefaultMessage(),
                                "Invalid value"
                        ),
                        (first, second) -> first + "; " + second,
                        LinkedHashMap::new
                ));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed"
        );

        problem.setTitle("Bad request");
        problem.setType(URI.create("urn:problem-type:validation-failed"));
        problem.setProperty("errors", errors);

        log.debug("Request validation failed for fields {}", errors.keySet());

        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleExceptions(Exception ex){

        log.error("Unhandled exception processing request", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );

        problem.setTitle("Internal server error");
        problem.setType(URI.create("urn:problem-type:internal-error"));

        return problem;
    }
}
