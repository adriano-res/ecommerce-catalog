package com.ecommerce.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record CreateCategoryRequest(
    @NotBlank(message = "Name must not be blank")
    String name,

    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase alphanumeric with hyphens")
    String slug,

    String parentId,

    List<AttributeDefinition> attributeDefinitions
) {}
