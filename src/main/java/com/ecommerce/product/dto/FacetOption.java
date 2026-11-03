package com.ecommerce.product.dto;

/** Facet option representing a distinct attribute value and its occurrence count. */
public record FacetOption(String value, long count) {}
