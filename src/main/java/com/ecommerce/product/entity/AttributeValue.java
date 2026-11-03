package com.ecommerce.product.entity;

import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Physical representation of a heterogeneous attribute under the Attribute Pattern.
 *
 * @param key attribute identifier (indexed as k)
 * @param value dynamic value (indexed as v)
 * @param unit optional measurement unit (e.g., "GB", "V")
 */
public record AttributeValue(
    @Field("k") String key,
    @Field("v") Object value,
    @Field("unit") String unit) {}
