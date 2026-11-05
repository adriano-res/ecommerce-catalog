package com.ecommerce.product.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Value object representing product imagery metadata.
 *
 * @param id unique identifier of the image
 * @param url image source URL
 * @param alt descriptive alternative text for accessibility
 */
@Schema(description = "Product image metadata")
public record ProductImage(
    @Schema(description = "Unique image identifier UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    String id,
    @Schema(description = "Image storage URL", example = "https://cdn.store.com/products/gx24-front.jpg")
    String url,
    @Schema(description = "Descriptive alternative text for accessibility", example = "Front view of Galaxy X24")
    String alt) {

  public ProductImage {
    if (id == null || id.isBlank()) {
      id = UUID.randomUUID().toString();
    }
  }

  public ProductImage(String url, String alt) {
    this(UUID.randomUUID().toString(), url, alt);
  }
}
