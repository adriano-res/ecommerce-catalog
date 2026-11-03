package com.ecommerce.product.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregated facet counts for dynamic attributes and predefined price ranges. */
public record SearchFacetResponse(
    @JsonAnyGetter @JsonAnySetter Map<String, List<FacetOption>> attributes,
    @JsonProperty("priceRanges") List<PriceRangeFacet> priceRanges) {

  public SearchFacetResponse {
    attributes =
        attributes != null
            ? Collections.unmodifiableMap(new LinkedHashMap<>(attributes))
            : Collections.emptyMap();
    priceRanges = priceRanges != null ? List.copyOf(priceRanges) : Collections.emptyList();
  }
}
