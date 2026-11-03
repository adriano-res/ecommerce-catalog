package com.ecommerce.product.validation;

import com.ecommerce.category.dto.AttributeDefinition;
import com.ecommerce.shared.error.DomainException;
import com.ecommerce.shared.error.FailureCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Validates dynamic product attributes against the effective attribute definitions of the
 * product's category.
 */
@Component
public class AttributeValidator {

  public void validate(
      Map<String, Object> attributes, List<AttributeDefinition> effectiveDefinitions) {
    List<AttributeDefinition> definitions =
        (effectiveDefinitions != null) ? effectiveDefinitions : Collections.emptyList();
    Map<String, AttributeDefinition> defMap = new LinkedHashMap<>();
    for (AttributeDefinition def : definitions) {
      if (def.key() != null) {
        defMap.put(def.key(), def);
      }
    }

    Map<String, Object> attrs = (attributes != null) ? attributes : Collections.emptyMap();
    List<String> errors = new ArrayList<>();

    checkUndeclaredAttributes(attrs, defMap, errors);
    checkRequiredAttributes(attrs, definitions, errors);
    validatePresentAttributes(attrs, defMap, errors);

    if (!errors.isEmpty()) {
      throw new DomainException(
          FailureCode.INVALID_PRODUCT_ATTRIBUTES,
          "Invalid product attributes: " + String.join("; ", errors),
          Map.of("errors", errors));
    }
  }

  private void checkUndeclaredAttributes(
      Map<String, Object> attrs, Map<String, AttributeDefinition> defMap, List<String> errors) {
    for (String key : attrs.keySet()) {
      if (!defMap.containsKey(key)) {
        errors.add("Attribute '" + key + "' is not declared in category");
      }
    }
  }

  private void checkRequiredAttributes(
      Map<String, Object> attrs, List<AttributeDefinition> definitions, List<String> errors) {
    for (AttributeDefinition def : definitions) {
      if (def.required()) {
        Object val = attrs.get(def.key());
        if (val == null || (val instanceof String s && s.isBlank())) {
          errors.add("Required attribute '" + def.key() + "' is missing");
        }
      }
    }
  }

  private void validatePresentAttributes(
      Map<String, Object> attrs, Map<String, AttributeDefinition> defMap, List<String> errors) {
    for (Map.Entry<String, Object> entry : attrs.entrySet()) {
      String key = entry.getKey();
      Object val = entry.getValue();
      AttributeDefinition def = defMap.get(key);

      if (def == null || val == null) {
        continue;
      }

      validateValueType(key, val, def, errors);
    }
  }

  private void validateValueType(
      String key, Object val, AttributeDefinition def, List<String> errors) {
    switch (def.type()) {
      case ENUM -> {
        String strVal = String.valueOf(val);
        List<String> allowed = def.allowedValues();
        if (allowed == null || !allowed.contains(strVal)) {
          errors.add(
              "Attribute '"
                  + key
                  + "' has invalid value '"
                  + strVal
                  + "'. Allowed values: "
                  + (allowed != null ? allowed : List.of()));
        }
      }
      case NUMBER -> {
        if (!(val instanceof Number)) {
          if (val instanceof String s && !s.isBlank()) {
            try {
              Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
              errors.add(
                  "Attribute '" + key + "' must be a numeric value, got: '" + val + "'");
            }
          } else {
            errors.add("Attribute '" + key + "' must be a numeric value, got: " + val);
          }
        }
      }
      case BOOLEAN -> {
        if (!(val instanceof Boolean)
            && !(val instanceof String s
                && (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")))) {
          errors.add("Attribute '" + key + "' must be a boolean value, got: " + val);
        }
      }
      case STRING -> {
        if (!(val instanceof String)) {
          errors.add("Attribute '" + key + "' must be a string, got: " + val);
        }
      }
    }
  }
}
