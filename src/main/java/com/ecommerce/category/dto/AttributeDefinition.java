package com.ecommerce.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Attribute definition defining data type, validation rules, and facet filterability")
public record AttributeDefinition(
    @Schema(description = "Unique attribute key identifier", example = "ram")
    @NotBlank(message = "Attribute key must not be blank")
    String key,

    @Schema(description = "Human-readable attribute display label", example = "Memória RAM")
    @NotBlank(message = "Attribute label must not be blank")
    String label,

    @Schema(description = "Data type for attribute values", example = "ENUM")
    @NotNull(message = "Attribute type must not be null")
    AttributeType type,

    @Schema(description = "Optional unit of measurement", example = "GB")
    String unit,

    @Schema(description = "Whether this attribute is mandatory when creating/updating products", example = "true")
    boolean required,

    @Schema(description = "Whether this attribute is indexed for faceted search filters", example = "true")
    boolean filterable,

    @Schema(description = "Allowed values when attribute type is ENUM", example = "[\"4GB\", \"8GB\", \"12GB\", \"16GB\"]")
    List<String> allowedValues
) {
  public AttributeDefinition {
    allowedValues = (allowedValues != null) ? List.copyOf(allowedValues) : List.of();
  }

  public enum AttributeType {
    STRING,
    NUMBER,
    BOOLEAN,
    ENUM
  }
}
