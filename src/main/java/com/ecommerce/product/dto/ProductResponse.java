package com.ecommerce.product.dto;

import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.shared.money.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Response DTO representing full product details. */
@Schema(description = "Detailed product response representation")
public record ProductResponse(
    @Schema(description = "Unique product identifier", example = "6720f1a2c3d4e5f600000001")
    String id,

    @Schema(description = "Stock Keeping Unit code", example = "SMART-GX24-8-256-BLK")
    String sku,

    @Schema(description = "Unique URL slug", example = "galaxy-x24-8gb-256gb-preto")
    String slug,

    @Schema(description = "Product display name", example = "Galaxy X24 8GB 256GB Preto")
    String name,

    @Schema(description = "Brand name", example = "Samsung")
    String brand,

    @Schema(description = "Detailed description", example = "Smartphone com tela AMOLED de 6.5 polegadas...")
    String description,

    @Schema(description = "Direct category ID", example = "6720f0000000000000000003")
    String categoryId,

    @Schema(description = "Denormalized category ancestor path IDs", example = "[\"6720f0000000000000000001\", \"6720f0000000000000000002\", \"6720f0000000000000000003\"]")
    List<String> categoryPath,

    @Schema(description = "Product lifecycle status", example = "ACTIVE")
    ProductStatus status,

    @Schema(description = "Monetary price representation")
    Money price,

    @Schema(
        description = "Dynamic attributes in Attribute Pattern format",
        example = "{\"ram\": \"8GB\", \"storageGb\": 256, \"color\": \"Preto\", \"is5g\": true}")
    Map<String, Object> attributes,

    @Schema(description = "List of product imagery")
    List<ProductImage> images,

    @Schema(description = "Optimistic locking document version", example = "1")
    Long version,

    @Schema(description = "Creation timestamp")
    Instant createdAt,

    @Schema(description = "Last modification timestamp")
    Instant updatedAt) {}
