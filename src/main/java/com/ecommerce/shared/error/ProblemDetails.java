package com.ecommerce.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class ProblemDetails {

  public static final String ERROR_TYPE_PREFIX = "https://api.ecommerce.example.com/errors/";
  public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
  public static final String MDC_CORRELATION_ID = "correlationId";

  private ProblemDetails() {}

  public static ProblemDetail build(
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

  public static String resolveCorrelationId(HttpServletRequest request) {
    String correlationId = MDC.get(MDC_CORRELATION_ID);
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = request.getHeader(CORRELATION_ID_HEADER);
    }
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }
    return correlationId;
  }
}
