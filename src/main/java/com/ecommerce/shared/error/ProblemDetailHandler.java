package com.ecommerce.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ProblemDetailHandler {

  private static final Logger log = LoggerFactory.getLogger(ProblemDetailHandler.class);
  private static final String ERROR_TYPE_PREFIX = "https://api.ecommerce.example.com/errors/";
  private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
  private static final String MDC_CORRELATION_ID = "correlationId";

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ProblemDetail> handleDomainException(
      DomainException ex, HttpServletRequest request) {
    HttpStatus status = ex.getFailureCode().getHttpStatus();
    ProblemDetail problem = buildProblemDetail(
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
    ProblemDetail problem = buildProblemDetail(
        status,
        failureCode,
        "Validation failed for one or more fields",
        request);

    List<InvalidParam> invalidParams = ex.getBindingResult().getFieldErrors().stream()
        .map(fieldError -> new InvalidParam(fieldError.getField(), fieldError.getDefaultMessage()))
        .toList();
    problem.setProperty("invalidParams", invalidParams);

    log.warn("Validation error: {}", invalidParams);
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> handleOptimisticLocking(
      OptimisticLockingFailureException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.CONFLICT;
    FailureCode failureCode = FailureCode.CONCURRENT_MODIFICATION;
    ProblemDetail problem = buildProblemDetail(
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
    ProblemDetail problem = buildProblemDetail(
        status,
        failureCode,
        ex.getMessage(),
        request);
    log.warn("Illegal argument: {}", ex.getMessage());
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ProblemDetail> handleNoResourceFound(
      NoResourceFoundException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.NOT_FOUND;
    FailureCode failureCode = FailureCode.RESOURCE_NOT_FOUND;
    ProblemDetail problem = buildProblemDetail(
        status,
        failureCode,
        ex.getMessage(),
        request);
    log.warn("Resource not found: {}", ex.getMessage());
    return ResponseEntity.status(status).body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnhandled(
      Exception ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    FailureCode failureCode = FailureCode.INTERNAL_ERROR;
    ProblemDetail problem = buildProblemDetail(
        status,
        failureCode,
        "An unexpected error occurred. Please contact support with correlationId.",
        request);
    log.error("Unhandled error: ", ex);
    return ResponseEntity.status(status).body(problem);
  }

  private ProblemDetail buildProblemDetail(
      HttpStatus status,
      FailureCode failureCode,
      String detail,
      HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(URI.create(ERROR_TYPE_PREFIX + failureCode.name()));
    problem.setTitle(status.getReasonPhrase());
    problem.setInstance(URI.create(request.getRequestURI()));

    String correlationId = resolveCorrelationId(request);
    problem.setProperty("failureCode", failureCode.name());
    problem.setProperty("timestamp", Instant.now().toString());
    problem.setProperty("correlationId", correlationId);
    return problem;
  }

  private String resolveCorrelationId(HttpServletRequest request) {
    String correlationId = MDC.get(MDC_CORRELATION_ID);
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = request.getHeader(CORRELATION_ID_HEADER);
    }
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }
    return correlationId;
  }

  public record InvalidParam(String name, String reason) {}
}
