package com.ecommerce.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Hierarchical category node containing children for taxonomy tree rendering")
public record CategoryTreeResponse(
    @Schema(description = "Unique category identifier", example = "6720f0000000000000000001")
    String id,

    @Schema(description = "Category name", example = "Eletrônicos")
    String name,

    @Schema(description = "Unique category URL slug", example = "eletronicos")
    String slug,

    @Schema(description = "Parent category identifier or null if root", example = "null")
    String parentId,

    @Schema(description = "Attribute definitions declared on this node")
    List<AttributeDefinition> attributeDefinitions,

    @Schema(description = "Nested child categories")
    List<CategoryTreeResponse> children
) {}
