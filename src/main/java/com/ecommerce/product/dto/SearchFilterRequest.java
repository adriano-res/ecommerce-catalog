package com.ecommerce.product.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Request parameters for faceted product search and pagination. */
public record SearchFilterRequest(
    String q,
    String category,
    Map<String, List<String>> attributes,
    BigDecimal priceMin,
    BigDecimal priceMax,
    SearchSort sort,
    String cursor,
    Integer limit) {

  public SearchFilterRequest {
    limit = (limit != null && limit > 0) ? Math.min(limit, 50) : 20;
    attributes = (attributes != null) ? Map.copyOf(attributes) : Collections.emptyMap();
  }
}
