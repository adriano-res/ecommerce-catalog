package com.ecommerce.product.dto;

import com.ecommerce.shared.money.Money;
import io.swagger.v3.oas.annotations.media.Schema;

/** Search result projection item representing product details in search listings. */
@Schema(description = "Product summary item returned in faceted search results")
public record ProductSearchItemResponse(
    @Schema(description = "Product unique identifier", example = "6720f1a2c3d4e5f600000001")
    String id,

    @Schema(description = "Stock Keeping Unit code", example = "SMART-GX24-8-256-BLK")
    String sku,

    @Schema(description = "Unique URL slug", example = "galaxy-x24-8gb-256gb-preto")
    String slug,

    @Schema(description = "Product display name", example = "Galaxy X24 8GB 256GB Preto")
    String name,

    @Schema(description = "Brand name", example = "Samsung")
    String brand,

    @Schema(description = "Product price")
    Money price,

    @Schema(description = "Primary thumbnail image URL", example = "https://cdn.store.com/products/gx24-front.jpg")
    String thumbnailUrl) {}
