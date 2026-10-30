package com.ecommerce.shared.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class ProblemDetailHandlerTest {

  private ProblemDetailHandler handler;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    handler = new ProblemDetailHandler();
    request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/v1/test");
    when(request.getHeader("X-Correlation-Id")).thenReturn("test-correlation-id-123");
  }

  @Test
  @DisplayName("Should return 404 ProblemDetail when DomainException thrown")
  void shouldReturnNotFoundProblemDetail_whenDomainExceptionThrown() {
    DomainException ex = new DomainException(FailureCode.PRODUCT_NOT_FOUND, "Product with SKU NK-001 not found");

    ResponseEntity<ProblemDetail> response = handler.handleDomainException(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    ProblemDetail problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getTitle()).isEqualTo("Not Found");
    assertThat(problem.getDetail()).isEqualTo("Product with SKU NK-001 not found");
    assertThat(problem.getProperties()).containsEntry("failureCode", "PRODUCT_NOT_FOUND");
    assertThat(problem.getProperties()).containsEntry("correlationId", "test-correlation-id-123");
    assertThat(problem.getType().toString()).isEqualTo("https://api.ecommerce.example.com/errors/PRODUCT_NOT_FOUND");
  }

  @Test
  @DisplayName("Should include details map when DomainException has details")
  void shouldIncludeDetailsMap_whenDomainExceptionHasDetails() {
    DomainException ex = new DomainException(
        FailureCode.INVALID_PRODUCT_ATTRIBUTES,
        "Attributes validation failed",
        Map.of("field", "size", "reason", "unsupported"));

    ResponseEntity<ProblemDetail> response = handler.handleDomainException(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    ProblemDetail problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getProperties()).containsKey("details");
    @SuppressWarnings("unchecked")
    Map<String, Object> details = (Map<String, Object>) problem.getProperties().get("details");
    assertThat(details).containsEntry("field", "size");
  }

  @Test
  @DisplayName("Should return 400 ProblemDetail with invalidParams when validation fails")
  void shouldReturnBadRequestWithInvalidParams_whenValidationFails() {
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError fieldError = new FieldError("productRequest", "sku", "must not be blank");
    ObjectError globalError = new ObjectError("productRequest", "Price must be greater than discount");

    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
    when(bindingResult.getGlobalErrors()).thenReturn(List.of(globalError));

    ResponseEntity<ProblemDetail> response = handler.handleValidationException(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ProblemDetail problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getProperties()).containsEntry("failureCode", "VALIDATION_ERROR");
    @SuppressWarnings("unchecked")
    List<ProblemDetailHandler.InvalidParam> params =
        (List<ProblemDetailHandler.InvalidParam>) problem.getProperties().get("invalidParams");
    assertThat(params).hasSize(2);
    assertThat(params.get(0).name()).isEqualTo("sku");
    assertThat(params.get(0).reason()).isEqualTo("must not be blank");
    assertThat(params.get(1).name()).isEqualTo("productRequest");
    assertThat(params.get(1).reason()).isEqualTo("Price must be greater than discount");
  }

  @Test
  @DisplayName("Should return 409 Conflict ProblemDetail when OptimisticLockingFailureException occurs")
  void shouldReturnConflictProblemDetail_whenOptimisticLockingConflictOccurs() {
    OptimisticLockingFailureException ex =
        new OptimisticLockingFailureException("Version conflict");

    ResponseEntity<ProblemDetail> response = handler.handleOptimisticLocking(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    ProblemDetail problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getProperties()).containsEntry("failureCode", "CONCURRENT_MODIFICATION");
  }

  @Test
  @DisplayName("Should return 405 Method Not Allowed ProblemDetail when HttpRequestMethodNotSupportedException occurs")
  void shouldReturnMethodNotAllowedProblemDetail_whenMethodNotSupported() {
    HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("GET");

    ResponseEntity<ProblemDetail> response = handler.handleMethodNotSupported(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    ProblemDetail problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getProperties()).containsEntry("failureCode", "METHOD_NOT_ALLOWED");
  }

  @Test
  @DisplayName("Should return 400 Bad Request ProblemDetail when IllegalArgumentException occurs")
  void shouldReturnBadRequestProblemDetail_whenIllegalArgumentExceptionOccurs() {
    IllegalArgumentException ex = new IllegalArgumentException("Invalid argument provided");

    ResponseEntity<ProblemDetail> response = handler.handleIllegalArgument(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ProblemDetail problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getDetail()).isEqualTo("Invalid argument provided");
    assertThat(problem.getProperties()).containsEntry("failureCode", "VALIDATION_ERROR");
  }

  @Test
  @DisplayName("Should return 404 Not Found ProblemDetail when NoResourceFoundException occurs")
  void shouldReturnNotFoundProblemDetail_whenNoResourceFoundExceptionOccurs() {
    NoResourceFoundException ex = mock(NoResourceFoundException.class);
    when(ex.getMessage()).thenReturn("Static resource not found");

    ResponseEntity<ProblemDetail> response = handler.handleNoResourceFound(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    ProblemDetail problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getDetail()).isEqualTo("Static resource not found");
    assertThat(problem.getProperties()).containsEntry("failureCode", "RESOURCE_NOT_FOUND");
  }

  @Test
  @DisplayName("Should return 500 Internal Server Error ProblemDetail when unhandled Exception occurs")
  void shouldReturnInternalServerErrorProblemDetail_whenUnhandledExceptionOccurs() {
    RuntimeException ex = new RuntimeException("Database connection timeout");

    ResponseEntity<ProblemDetail> response = handler.handleUnhandled(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    ProblemDetail problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
    assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred. Please contact support with correlationId.");
    assertThat(problem.getProperties()).containsEntry("failureCode", "INTERNAL_ERROR");
    assertThat(problem.getProperties()).containsEntry("correlationId", "test-correlation-id-123");
    assertThat(problem.getDetail()).doesNotContain("Database connection timeout");
  }
}
