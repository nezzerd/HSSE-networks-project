package com.searchengine.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("about:blank"));
        pd.setTitle("Validation failed");
        pd.setDetail(ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .findFirst()
            .orElse("Invalid input"));
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Constraint violation");
        pd.setDetail(ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .findFirst()
            .orElse("Invalid input"));
        return pd;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleMethodValidation(HandlerMethodValidationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation failed");
        pd.setDetail("One or more request parameters are invalid");
        return pd;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Missing parameter");
        pd.setDetail("Required parameter is missing: " + ex.getParameterName());
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Malformed request");
        pd.setDetail("Request body is missing or malformed");
        return pd;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED);
        pd.setTitle("Method not allowed");
        pd.setDetail("HTTP method not supported for this endpoint");
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Type mismatch");
        pd.setDetail("Invalid value for parameter: " + ex.getName());
        return pd;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNotFound(NoResourceFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Not found");
        pd.setDetail("Resource not found");
        return pd;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ProblemDetail handleRateLimit(RateLimitExceededException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        pd.setTitle("Too many requests");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Internal server error");
        pd.setDetail("An unexpected error occurred");
        return pd;
    }
}
