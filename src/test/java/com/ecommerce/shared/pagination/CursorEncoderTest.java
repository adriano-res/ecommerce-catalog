package com.ecommerce.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CursorEncoderTest {

  @Test
  @DisplayName("Should encode and decode deterministically")
  void shouldEncodeAndDecodeDeterministically_whenValidSortValueAndIdProvided() {
    String sortValue = "199990";
    String tieBreakerId = "67211bf7e8293c001248ab91";

    String encoded = CursorEncoder.encode(sortValue, tieBreakerId);
    assertThat(encoded).isNotEmpty();
    assertThat(encoded).doesNotContain("="); // No padding

    CursorEncoder.Cursor decoded = CursorEncoder.decode(encoded);
    assertThat(decoded.sortValue()).isEqualTo(sortValue);
    assertThat(decoded.tieBreakerId()).isEqualTo(tieBreakerId);
  }

  @Test
  @DisplayName("Should reject null or blank cursor on decode")
  void shouldThrowIllegalArgumentException_whenCursorIsNullOrBlank() {
    assertThatThrownBy(() -> CursorEncoder.decode(null))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> CursorEncoder.decode("   "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should reject malformed base64 cursor")
  void shouldThrowIllegalArgumentException_whenCursorIsMalformedBase64() {
    assertThatThrownBy(() -> CursorEncoder.decode("not-a-valid-base64!@#$"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should reject base64 string without separator")
  void shouldThrowIllegalArgumentException_whenCursorLacksSeparator() {
    String encodedWithoutSep = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("no_separator".getBytes());
    assertThatThrownBy(() -> CursorEncoder.decode(encodedWithoutSep))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Malformed cursor format");
  }
}
