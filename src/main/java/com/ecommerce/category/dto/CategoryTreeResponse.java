package com.ecommerce.category.dto;

import java.util.List;

public record CategoryTreeResponse(
    String id,
    String name,
    String slug,
    String parentId,
    List<AttributeDefinition> attributeDefinitions,
    List<CategoryTreeResponse> children
) {}
