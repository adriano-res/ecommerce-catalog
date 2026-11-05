package com.ecommerce.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Aggregated count within a predefined monetary price range. */
@Schema(description = "Price range bucket and its item count")
public record PriceRangeFacet(
    @Schema(description = "Lower bound decimal string (inclusive)", example = "2000.00")
    String from,

    @Schema(description = "Upper bound decimal string (inclusive), or null for open range", example = "3999.99")
    String to,

    @Schema(description = "Total matching items in this price bucket", example = "36")
    long count) {}
