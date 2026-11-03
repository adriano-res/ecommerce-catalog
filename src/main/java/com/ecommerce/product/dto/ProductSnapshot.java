package com.ecommerce.product.dto;

import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.product.entity.ProductStatus;
import com.ecommerce.shared.audit.AuditMetadata;
import com.ecommerce.shared.money.Money;
import java.util.List;
import java.util.Map;

/** Read-only snapshot of product state for external module facades. */
public record ProductSnapshot(
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
    AuditMetadata audit) {}
