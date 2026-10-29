package com.ecommerce.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Stable, machine-readable failure code taxonomy matching API specification.
 */
public enum FailureCode {
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
  CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND),
  PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
  DUPLICATE_SKU(HttpStatus.CONFLICT),
  DUPLICATE_SLUG(HttpStatus.CONFLICT),
  CATEGORY_NOT_EMPTY(HttpStatus.CONFLICT),
  CONCURRENT_MODIFICATION(HttpStatus.CONFLICT),
  INVALID_ATTRIBUTE_DEFINITION(HttpStatus.UNPROCESSABLE_ENTITY),
  INVALID_PRODUCT_ATTRIBUTES(HttpStatus.UNPROCESSABLE_ENTITY),
  CURRENCY_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

  private final HttpStatus httpStatus;

  FailureCode(HttpStatus httpStatus) {
    this.httpStatus = httpStatus;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
