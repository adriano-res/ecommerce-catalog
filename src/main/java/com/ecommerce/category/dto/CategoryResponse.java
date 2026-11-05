package com.ecommerce.category.dto;

import com.ecommerce.shared.audit.AuditMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Category details including hierarchy and effective inherited attribute definitions")
public record CategoryResponse(
    @Schema(description = "Unique category identifier", example = "6720f0000000000000000003")
    String id,

    @Schema(description = "Category name", example = "Smartphones")
    String name,

    @Schema(description = "Unique URL slug", example = "smartphones")
    String slug,

    @Schema(description = "Immediate parent category ID", example = "6720f0000000000000000002")
    String parentId,

    @Schema(description = "Ordered list of ancestor category IDs from root to immediate parent", example = "[\"6720f0000000000000000001\", \"6720f0000000000000000002\"]")
    List<String> ancestors,

    @Schema(description = "Locally declared attribute definitions")
    List<AttributeDefinition> attributeDefinitions,

    @Schema(description = "Effective attribute definitions merged from root ancestor down to this category")
    @JsonProperty("effectiveAttributeDefinitions")
    List<AttributeDefinition> effectiveAttributeDefinitions,

    @Schema(description = "Creation and last modification audit metadata")
    AuditMetadata audit
) {}
