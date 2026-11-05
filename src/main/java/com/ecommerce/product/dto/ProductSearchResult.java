package com.ecommerce.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;

/** Consolidated response for faceted product search including items, total count, facets, and cursor. */
@Schema(description = "Consolidated result of faceted product search containing paginated items, facet counters, and pagination cursor")
public record ProductSearchResult(
    @Schema(description = "Matching products for current page")
    List<ProductSearchItemResponse> items,

    @Schema(description = "Total count of matching products matching search criteria across all pages", example = "53")
    long totalCount,

    @Schema(description = "Aggregated facet counts by attribute and price range")
    SearchFacetResponse facets,

    @Schema(description = "Opaque Base64URL cursor token to retrieve the next page of results, or null if on last page", example = "ZXlKcElqb3pNVEk1T1RBc0ltbG1JanBv...")
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
