package com.ecommerce.shared.util;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtils {

  private SlugUtils() {}

  public static String toSlug(String text) {
    if (text == null || text.isBlank()) {
      return "";
    }
    String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
    return normalized.toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9\\s-]", "")
        .trim()
        .replaceAll("\\s+", "-");
  }

  public static String resolveSlug(String name, String customSlug) {
    if (customSlug != null && !customSlug.isBlank()) {
      return customSlug.trim().toLowerCase(Locale.ROOT);
    }
    return toSlug(name);
  }
}
