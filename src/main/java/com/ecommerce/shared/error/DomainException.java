package com.ecommerce.shared.error;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;

/**
 * Base domain exception containing a semantic failure code and associated HTTP status.
 */
public class DomainException extends RuntimeException {

  private final FailureCode failureCode;
  private final Map<String, Object> details;

  public DomainException(FailureCode failureCode, String message) {
    this(failureCode, message, Collections.emptyMap());
  }

  public DomainException(FailureCode failureCode, String message, Map<String, Object> details) {
    super(message);
    this.failureCode = Objects.requireNonNull(failureCode, "FailureCode must not be null");
    this.details = details != null ? Map.copyOf(details) : Collections.emptyMap();
  }

  public FailureCode getFailureCode() {
    return failureCode;
  }

  public HttpStatus getHttpStatus() {
    return failureCode.getHttpStatus();
  }

  public Map<String, Object> getDetails() {
    return details;
  }
}
