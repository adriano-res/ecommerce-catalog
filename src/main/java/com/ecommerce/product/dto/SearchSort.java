package com.ecommerce.product.dto;

import java.util.Locale;

/** Sorting criteria for catalog product searches. */
public enum SearchSort {
  RELEVANCE,
  PRICE_ASC,
  PRICE_DESC,
  NEWEST;

  public static SearchSort fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim().toUpperCase(Locale.ROOT).replace("-", "_");
    return switch (normalized) {
      case "PRICEASC", "PRICE_ASC" -> PRICE_ASC;
      case "PRICEDESC", "PRICE_DESC" -> PRICE_DESC;
      case "NEWEST" -> NEWEST;
      case "RELEVANCE" -> RELEVANCE;
      default -> null;
    };
  }
}
