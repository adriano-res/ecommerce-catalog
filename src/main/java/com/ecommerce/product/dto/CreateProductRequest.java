package com.ecommerce.product.dto;

import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.shared.money.Money;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/** Request DTO for creating a new catalog product. */
public record CreateProductRequest(
    @NotBlank(message = "SKU must not be blank") String sku,
    String slug,
    @NotBlank(message = "Name must not be blank") String name,
    String brand,
    String description,
    @NotBlank(message = "CategoryId must not be blank") String categoryId,
    @NotNull(message = "Price must not be null") Money price,
    Map<String, Object> attributes,
    List<ProductImage> images) {

  public CreateProductRequest {
    attributes = (attributes != null) ? Map.copyOf(attributes) : Map.of();
    images = (images != null) ? List.copyOf(images) : List.of();
  }
}
