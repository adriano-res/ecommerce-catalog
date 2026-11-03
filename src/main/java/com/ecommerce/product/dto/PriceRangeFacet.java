package com.ecommerce.product.dto;

/** Aggregated count within a predefined monetary price range. */
public record PriceRangeFacet(String from, String to, long count) {}
