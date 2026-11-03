package com.ecommerce.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlugUtilsTest {

  @Test
  @DisplayName("Should use trimmed lower case custom slug when custom slug is provided")
  void shouldUseCustomSlug_whenCustomSlugProvided() {
    String result = SlugUtils.resolveSlug("Smartphone Pro 128GB", "  custom-phone-slug  ");
    assertThat(result).isEqualTo("custom-phone-slug");
  }

  @Test
  @DisplayName("Should generate slug from name when custom slug is null")
  void shouldGenerateSlugFromName_whenCustomSlugIsNull() {
    String result = SlugUtils.resolveSlug("Computadores & Informática", null);
    assertThat(result).isEqualTo("computadores-informatica");
  }

  @Test
  @DisplayName("Should generate slug from name when custom slug is blank")
  void shouldGenerateSlugFromName_whenCustomSlugIsBlank() {
    String result = SlugUtils.resolveSlug("Camiseta Masculina Básica - Algodão", "   ");
    assertThat(result).isEqualTo("camiseta-masculina-basica---algodao");
  }

  @Test
  @DisplayName("Should remove special characters and accents")
  void shouldRemoveAccentsAndSpecialCharacters_whenGeneratingSlug() {
    String result = SlugUtils.toSlug("Ação & Reação 100% Eficaz!");
    assertThat(result).isEqualTo("acao-reacao-100-eficaz");
  }

  @Test
  @DisplayName("Should return empty string when input is null or blank")
  void shouldReturnEmptyString_whenInputIsNullOrEmpty() {
    assertThat(SlugUtils.toSlug(null)).isEmpty();
    assertThat(SlugUtils.toSlug("   ")).isEmpty();
  }
}
