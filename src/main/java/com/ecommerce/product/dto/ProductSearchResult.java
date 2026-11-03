package com.ecommerce.product.dto;

import java.util.Collections;
import java.util.List;

/** Consolidated response for faceted product search including items, total count, facets, and cursor. */
public record ProductSearchResult(
    List<ProductSearchItemResponse> items,
    long totalCount,
    SearchFacetResponse facets,
    String nextCursor) {

  public ProductSearchResult {
    items = (items != null) ? List.copyOf(items) : Collections.emptyList();
  }

  public static ProductSearchResult empty() {
    List<PriceRangeFacet> defaultRanges =
        List.of(
            new PriceRangeFacet("0.00", "1999.99", 0L),
            new PriceRangeFacet("2000.00", "3999.99", 0L),
            new PriceRangeFacet("4000.00", null, 0L));
    return new ProductSearchResult(
        Collections.emptyList(),
        0L,
        new SearchFacetResponse(Collections.emptyMap(), defaultRanges),
        null);
  }
}
