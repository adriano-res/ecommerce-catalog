package com.ecommerce.product.dto;

import com.ecommerce.shared.money.Money;

/** Search result projection item representing product details in search listings. */
public record ProductSearchItemResponse(
    String id,
    String sku,
    String slug,
    String name,
    String brand,
    Money price,
    String thumbnailUrl) {}
