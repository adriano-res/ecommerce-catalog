package com.ecommerce.product.entity;

/**
 * Value object representing product imagery metadata.
 *
 * @param url image source URL
 * @param alt descriptive alternative text for accessibility
 */
public record ProductImage(String url, String alt) {}
