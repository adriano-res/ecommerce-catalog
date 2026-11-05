package com.ecommerce.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Facet option representing a distinct attribute value and its occurrence count. */
@Schema(description = "Facet option representing a distinct attribute value and its matching count")
public record FacetOption(
    @Schema(description = "Distinct attribute value", example = "8GB")
    String value,
    @Schema(description = "Total matching items for this value in current search context", example = "42")
    long count) {}
