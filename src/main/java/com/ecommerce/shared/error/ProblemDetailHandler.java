package com.ecommerce.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ProblemDetailHandler {

  private static final Logger log = LoggerFactory.getLogger(ProblemDetailHandler.class);

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ProblemDetail> handleDomainException(
      DomainException ex, HttpServletRequest request) {
    HttpStatus status = ex.getFailureCode().getHttpStatus();
    ProblemDetail problem = ProblemDetails.build(
        status,
        ex.getFailureCode(),
        ex.getMessage(),
        request);
    if (ex.getDetails() != null && !ex.getDetails().isEmpty()) {
      problem.setProperty("details", ex.getDetails());
    }
    log.warn("Domain exception: failureCode={}, message={}", ex.getFailureCode(), ex.getMessage());
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    FailureCode failureCode = FailureCode.VALIDATION_ERROR;
    ProblemDetail problem = ProblemDetails.build(
        status,
        failureCode,
        "Validation failed for one or more fields",
        request);

    List<InvalidParam> invalidParams = new ArrayList<>();
    ex.getBindingResult().getFieldErrors().forEach(
        fieldError -> invalidParams.add(new InvalidParam(fieldError.getField(), fieldError.getDefaultMessage())));
    ex.getBindingResult().getGlobalErrors().forEach(
        globalError -> invalidParams.add(new InvalidParam(globalError.getObjectName(), globalError.getDefaultMessage())));

    problem.setProperty("invalidParams", invalidParams);

    log.warn("Validation error: {}", invalidParams);
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> handleOptimisticLocking(
      OptimisticLockingFailureException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.CONFLICT;
    FailureCode failureCode = FailureCode.CONCURRENT_MODIFICATION;
    ProblemDetail problem = ProblemDetails.build(
        status,
        failureCode,
        "Resource was modified by another transaction. Please reload and retry.",
        request);
    log.warn("Optimistic locking conflict: {}", ex.getMessage());
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetail> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    FailureCode failureCode = FailureCode.VALIDATION_ERROR;
    ProblemDetail problem = ProblemDetails.build(
        status,
        failureCode,
        ex.getMessage(),
        request);
    log.warn("Illegal argument: {}", ex.getMessage());
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    FailureCode failureCode = FailureCode.VALIDATION_ERROR;
    String message = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
    ProblemDetail problem = ProblemDetails.build(
        status,
        failureCode,
        message,
        request);
    log.warn("Malformed HTTP request body: {}", message);
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ProblemDetail> handleNoResourceFound(
      NoResourceFoundException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.NOT_FOUND;
    FailureCode failureCode = FailureCode.RESOURCE_NOT_FOUND;
    ProblemDetail problem = ProblemDetails.build(
        status,
        failureCode,
        ex.getMessage(),
        request);
    log.warn("Resource not found: {}", ex.getMessage());
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ProblemDetail> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
    FailureCode failureCode = FailureCode.METHOD_NOT_ALLOWED;
    ProblemDetail problem = ProblemDetails.build(
        status,
        failureCode,
        ex.getMessage(),
        request);
    log.warn("HTTP method not supported: {}", ex.getMessage());
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnhandled(
      Exception ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    FailureCode failureCode = FailureCode.INTERNAL_ERROR;
    ProblemDetail problem = ProblemDetails.build(
        status,
        failureCode,
        "An unexpected error occurred. Please contact support with correlationId.",
        request);
    log.error("Unhandled error: ", ex);
    return ResponseEntity.status(status).body(problem);
  }

  public record InvalidParam(String name, String reason) {}
}
