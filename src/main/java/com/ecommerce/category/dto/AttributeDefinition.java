package com.ecommerce.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AttributeDefinition(
    @NotBlank(message = "Attribute key must not be blank")
    String key,

    @NotBlank(message = "Attribute label must not be blank")
    String label,

    @NotNull(message = "Attribute type must not be null")
    AttributeType type,

    String unit,
    boolean required,
    boolean filterable,
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
