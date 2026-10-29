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
import org.springframework.web.bind.MethodArgumentNotValidException;

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
  void shouldReturnBadRequestWithInvalidParams_whenMethodArgumentNotValidException() {
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    FieldError fieldError = new FieldError("productRequest", "sku", "must not be blank");

    when(ex.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

    ResponseEntity<ProblemDetail> response = handler.handleValidationException(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    ProblemDetail problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getProperties()).containsEntry("failureCode", "VALIDATION_ERROR");
    @SuppressWarnings("unchecked")
    List<ProblemDetailHandler.InvalidParam> params =
        (List<ProblemDetailHandler.InvalidParam>) problem.getProperties().get("invalidParams");
    assertThat(params).hasSize(1);
    assertThat(params.get(0).name()).isEqualTo("sku");
    assertThat(params.get(0).reason()).isEqualTo("must not be blank");
  }

  @Test
  @DisplayName("Should return 409 Conflict ProblemDetail when OptimisticLockingFailureException occurs")
  void shouldReturnConflictProblemDetail_whenOptimisticLockingFailureException() {
    OptimisticLockingFailureException ex =
        new OptimisticLockingFailureException("Version conflict");

    ResponseEntity<ProblemDetail> response = handler.handleOptimisticLocking(ex, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    ProblemDetail problem = response.getBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getProperties()).containsEntry("failureCode", "CONCURRENT_MODIFICATION");
  }
}
