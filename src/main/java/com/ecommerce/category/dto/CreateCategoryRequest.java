package com.ecommerce.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

@Schema(description = "Request payload for creating a category in the taxonomy")
public record CreateCategoryRequest(
    @Schema(description = "Category display name", example = "Smartphones")
    @NotBlank(message = "Name must not be blank")
    String name,

    @Schema(description = "Unique category URL slug (lowercase alphanumeric with hyphens)", example = "smartphones")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase alphanumeric with hyphens")
    String slug,

    @Schema(description = "Optional parent category ID to establish hierarchical tree relationship", example = "6720f0000000000000000002")
    String parentId,

    @Schema(description = "List of local attribute definitions declared directly on this category")
    List<AttributeDefinition> attributeDefinitions
) {}
