package com.ecommerce.product.dto;

import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.shared.money.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/** Request DTO for creating a new catalog product. */
@Schema(description = "Request payload for creating a new product in the catalog")
public record CreateProductRequest(
    @Schema(description = "Unique Stock Keeping Unit code", example = "SMART-GX24-8-256-BLK")
    @NotBlank(message = "SKU must not be blank")
    String sku,

    @Schema(description = "Optional unique URL slug; generated automatically from name if omitted", example = "galaxy-x24-8gb-256gb-preto")
    String slug,

    @Schema(description = "Product display title", example = "Galaxy X24 8GB 256GB Preto")
    @NotBlank(message = "Name must not be blank")
    String name,

    @Schema(description = "Manufacturer or brand name", example = "Samsung")
    String brand,

    @Schema(description = "Detailed product marketing description", example = "Smartphone com tela AMOLED de 6.5 polegadas...")
    String description,

    @Schema(description = "Associated category identifier", example = "6720f0000000000000000003")
    @NotBlank(message = "CategoryId must not be blank")
    String categoryId,

    @Schema(description = "Product selling price")
    @NotNull(message = "Price must not be null")
    Money price,

    @Schema(
        description = "Dynamic category attributes following the Attribute Pattern",
        example = "{\"ram\": \"8GB\", \"storageGb\": 256, \"color\": \"Preto\", \"is5g\": true}")
    Map<String, Object> attributes,

    @Schema(description = "List of associated product images")
    List<ProductImage> images) {

  public CreateProductRequest {
    attributes = (attributes != null) ? Map.copyOf(attributes) : Map.of();
    images = (images != null) ? List.copyOf(images) : List.of();
  }
}
