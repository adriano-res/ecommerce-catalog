package com.ecommerce.category.dto;

import com.ecommerce.shared.audit.AuditMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CategoryResponse(
    String id,
    String name,
    String slug,
    String parentId,
    List<String> ancestors,
    List<AttributeDefinition> attributeDefinitions,
    @JsonProperty("effectiveAttributeDefinitions")
    List<AttributeDefinition> effectiveAttributeDefinitions,
    AuditMetadata audit
) {}
