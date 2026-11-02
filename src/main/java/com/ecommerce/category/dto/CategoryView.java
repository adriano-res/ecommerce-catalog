package com.ecommerce.category.dto;

import java.util.List;

public record CategoryView(
    String id,
    String name,
    String slug,
    String parentId,
    List<String> ancestors,
    List<AttributeDefinition> attributeDefinitions,
    List<AttributeDefinition> effectiveAttributes
) {}
