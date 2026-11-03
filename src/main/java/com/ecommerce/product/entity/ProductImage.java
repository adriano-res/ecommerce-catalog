package com.ecommerce.product.entity;

import java.util.UUID;

/**
 * Value object representing product imagery metadata.
 *
 * @param id unique identifier of the image
 * @param url image source URL
 * @param alt descriptive alternative text for accessibility
 */
public record ProductImage(String id, String url, String alt) {

  public ProductImage {
    if (id == null || id.isBlank()) {
      id = UUID.randomUUID().toString();
    }
  }

  public ProductImage(String url, String alt) {
    this(UUID.randomUUID().toString(), url, alt);
  }
}
