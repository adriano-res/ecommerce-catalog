package com.ecommerce.product.dto;

import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.shared.money.Money;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Response DTO representing full product details. */
public record ProductResponse(
    String id,
    String sku,
    String slug,
    String name,
    String brand,
    String description,
    String categoryId,
    List<String> categoryPath,
    ProductStatus status,
    Money price,
    Map<String, Object> attributes,
    List<ProductImage> images,
    Long version,
    Instant createdAt,
    Instant updatedAt) {}
