package com.ecommerce.shared.pagination;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * Deterministic URL-safe Base64 cursor encoder and decoder.
 */
public final class CursorEncoder {

  private static final String SEPARATOR = "::";

  private CursorEncoder() {}

  public record Cursor(String sortValue, String tieBreakerId) {
    public Cursor {
      Objects.requireNonNull(sortValue, "sortValue cannot be null");
      Objects.requireNonNull(tieBreakerId, "tieBreakerId cannot be null");
    }
  }

  public static String encode(String sortValue, String tieBreakerId) {
    if (sortValue == null || tieBreakerId == null) {
      throw new IllegalArgumentException("sortValue and tieBreakerId must not be null");
    }
    String raw = sortValue + SEPARATOR + tieBreakerId;
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  public static Cursor decode(String encodedCursor) {
    if (encodedCursor == null || encodedCursor.isBlank()) {
      throw new IllegalArgumentException("Cursor cannot be null or blank");
    }
    try {
      byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedCursor);
      String raw = new String(decodedBytes, StandardCharsets.UTF_8);
      int sepIndex = raw.indexOf(SEPARATOR);
      if (sepIndex <= 0 || sepIndex + SEPARATOR.length() >= raw.length()) {
        throw new IllegalArgumentException("Malformed cursor format");
      }
      String sortValue = raw.substring(0, sepIndex);
      String tieBreakerId = raw.substring(sepIndex + SEPARATOR.length());
      return new Cursor(sortValue, tieBreakerId);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Invalid base64url cursor: " + ex.getMessage(), ex);
    }
  }
}
