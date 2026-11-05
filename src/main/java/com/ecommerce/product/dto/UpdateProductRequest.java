package com.ecommerce.product.dto;

import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.shared.money.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/** Request DTO for partially updating product attributes and metadata. */
@Schema(description = "Request payload for updating an existing product's attributes or metadata")
public record UpdateProductRequest(
    @Schema(description = "Updated product display name", example = "Galaxy X24 8GB 256GB Preto (2026)")
    String name,

    @Schema(description = "Updated brand name", example = "Samsung")
    String brand,

    @Schema(description = "Updated detailed description", example = "Smartphone atualizado com tela AMOLED...")
    String description,

    @Schema(description = "Target category identifier if moving category", example = "6720f0000000000000000003")
    String categoryId,

    @Schema(description = "Updated price")
    Money price,

    @Schema(
        description = "Updated dynamic attributes matching category definitions",
        example = "{\"ram\": \"12GB\", \"storageGb\": 512, \"color\": \"Prata\", \"is5g\": true}")
    Map<String, Object> attributes,

    @Schema(description = "Updated list of images")
    List<ProductImage> images) {

  public UpdateProductRequest {
    attributes = (attributes != null) ? Map.copyOf(attributes) : null;
    images = (images != null) ? List.copyOf(images) : null;
  }
}
