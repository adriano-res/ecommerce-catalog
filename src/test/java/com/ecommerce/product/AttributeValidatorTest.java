package com.ecommerce.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecommerce.category.dto.AttributeDefinition;
import com.ecommerce.category.dto.AttributeDefinition.AttributeType;
import com.ecommerce.product.validation.AttributeValidator;
import com.ecommerce.shared.error.DomainException;
import com.ecommerce.shared.error.FailureCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AttributeValidatorTest {

  private AttributeValidator validator;
  private List<AttributeDefinition> definitions;

  @BeforeEach
  void setUp() {
    validator = new AttributeValidator();
    definitions =
        List.of(
            new AttributeDefinition(
                "ram", "RAM", AttributeType.ENUM, "GB", true, true, List.of("4GB", "8GB", "16GB")),
            new AttributeDefinition(
                "storageGb", "Storage", AttributeType.NUMBER, "GB", true, true, List.of()),
            new AttributeDefinition(
                "is5g", "5G Support", AttributeType.BOOLEAN, null, false, true, List.of()),
            new AttributeDefinition(
                "color", "Color", AttributeType.STRING, null, false, true, List.of()));
  }

  @Test
  @DisplayName("Should pass validation when all attributes are valid and required are present")
  void shouldPassValidation_whenAllAttributesAreValidAndRequiredArePresent() {
    Map<String, Object> attrs =
        Map.of(
            "ram", "8GB",
            "storageGb", 256,
            "is5g", true,
            "color", "Preto");

    validator.validate(attrs, definitions);
  }

  @Test
  @DisplayName("Should throw exception when required attribute is missing")
  void shouldThrowException_whenRequiredAttributeIsMissing() {
    Map<String, Object> attrs =
        Map.of(
            "ram", "8GB",
            "color", "Preto"); // missing storageGb

    assertThatThrownBy(() -> validator.validate(attrs, definitions))
        .isInstanceOf(DomainException.class)
        .satisfies(
            ex -> {
              DomainException dex = (DomainException) ex;
              assertThat(dex.getFailureCode()).isEqualTo(FailureCode.INVALID_PRODUCT_ATTRIBUTES);
              assertThat(dex.getMessage()).contains("Required attribute 'storageGb' is missing");
            });
  }

  @Test
  @DisplayName("Should throw exception when attribute is not declared in category")
  void shouldThrowException_whenAttributeIsNotDeclaredInCategory() {
    Map<String, Object> attrs =
        Map.of(
            "ram", "8GB",
            "storageGb", 256,
            "extraField", "not_allowed");

    assertThatThrownBy(() -> validator.validate(attrs, definitions))
        .isInstanceOf(DomainException.class)
        .satisfies(
            ex -> {
              DomainException dex = (DomainException) ex;
              assertThat(dex.getFailureCode()).isEqualTo(FailureCode.INVALID_PRODUCT_ATTRIBUTES);
              assertThat(dex.getMessage())
                  .contains("Attribute 'extraField' is not declared in category");
            });
  }

  @Test
  @DisplayName("Should throw exception when enum value is not in allowed values")
  void shouldThrowException_whenEnumValueIsNotInAllowedValues() {
    Map<String, Object> attrs =
        Map.of(
            "ram", "32GB", // not in 4GB, 8GB, 16GB
            "storageGb", 256);

    assertThatThrownBy(() -> validator.validate(attrs, definitions))
        .isInstanceOf(DomainException.class)
        .satisfies(
            ex -> {
              DomainException dex = (DomainException) ex;
              assertThat(dex.getFailureCode()).isEqualTo(FailureCode.INVALID_PRODUCT_ATTRIBUTES);
              assertThat(dex.getMessage()).contains("has invalid value '32GB'");
            });
  }

  @Test
  @DisplayName("Should throw exception when numeric attribute is not a number")
  void shouldThrowException_whenNumericAttributeIsNotANumber() {
    Map<String, Object> attrs =
        Map.of(
            "ram", "8GB",
            "storageGb", "not-a-number");

    assertThatThrownBy(() -> validator.validate(attrs, definitions))
        .isInstanceOf(DomainException.class)
        .satisfies(
            ex -> {
              DomainException dex = (DomainException) ex;
              assertThat(dex.getFailureCode()).isEqualTo(FailureCode.INVALID_PRODUCT_ATTRIBUTES);
              assertThat(dex.getMessage()).contains("must be a numeric value");
            });
  }

  @Test
  @DisplayName("Should throw exception when boolean attribute is not a boolean")
  void shouldThrowException_whenBooleanAttributeIsNotABoolean() {
    Map<String, Object> attrs =
        Map.of(
            "ram", "8GB",
            "storageGb", 256,
            "is5g", "not_boolean");

    assertThatThrownBy(() -> validator.validate(attrs, definitions))
        .isInstanceOf(DomainException.class)
        .satisfies(
            ex -> {
              DomainException dex = (DomainException) ex;
              assertThat(dex.getFailureCode()).isEqualTo(FailureCode.INVALID_PRODUCT_ATTRIBUTES);
              assertThat(dex.getMessage()).contains("must be a boolean value");
            });
  }

  @Test
  @DisplayName("Should throw exception when string attribute is not a string")
  void shouldThrowException_whenStringAttributeIsNotAString() {
    Map<String, Object> attrs =
        Map.of(
            "ram", "8GB",
            "storageGb", 256,
            "color", 12345);

    assertThatThrownBy(() -> validator.validate(attrs, definitions))
        .isInstanceOf(DomainException.class)
        .satisfies(
            ex -> {
              DomainException dex = (DomainException) ex;
              assertThat(dex.getFailureCode()).isEqualTo(FailureCode.INVALID_PRODUCT_ATTRIBUTES);
              assertThat(dex.getMessage()).contains("must be a string");
            });
  }

  @Test
  @DisplayName("Should pass validation when optional attributes are omitted")
  void shouldPassValidation_whenOptionalAttributesAreOmitted() {
    Map<String, Object> attrs =
        Map.of(
            "ram", "8GB",
            "storageGb", 128);

    validator.validate(attrs, definitions);
  }

  @Test
  @DisplayName("Should pass validation when numeric and boolean strings are provided")
  void shouldPassValidation_whenNumericAndBooleanStringsAreProvided() {
    Map<String, Object> attrs =
        Map.of(
            "ram", "8GB",
            "storageGb", "256",
            "is5g", "true");

    validator.validate(attrs, definitions);
  }
}
