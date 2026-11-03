package com.ecommerce.product.dto;

import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.shared.money.Money;
import java.util.List;
import java.util.Map;

/** Request DTO for partially updating product attributes and metadata. */
public record UpdateProductRequest(
    String name,
    String brand,
    String description,
    String categoryId,
    Money price,
    Map<String, Object> attributes,
    List<ProductImage> images) {

  public UpdateProductRequest {
    attributes = (attributes != null) ? Map.copyOf(attributes) : null;
    images = (images != null) ? List.copyOf(images) : null;
  }
}
