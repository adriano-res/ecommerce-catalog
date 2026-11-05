package com.ecommerce.shared.money;

import com.ecommerce.shared.error.CurrencyMismatchException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable Value Object representing monetary amounts with exact precision.
 * Internally stores amounts as a 64-bit long in minor currency units (cents),
 * prohibiting any use of floating-point types (float/double).
 */
@JsonSerialize(using = Money.MoneySerializer.class)
@JsonDeserialize(using = Money.MoneyDeserializer.class)
@Schema(
    description = "Monetary amount representation with currency and decimal string",
    example = "{\"amount\": \"3499.90\", \"currency\": \"BRL\"}")
public record Money(
    long amountMinor,
    Currency currency
) implements Comparable<Money> {

  public static final Currency BRL = Currency.getInstance("BRL");
  public static final Currency USD = Currency.getInstance("USD");

  public Money {
    Objects.requireNonNull(currency, "Currency must not be null");
    if (amountMinor < 0) {
      throw new IllegalArgumentException("Amount minor cannot be negative: " + amountMinor);
    }
  }

  public static Money of(String amount, String currencyCode) {
    Objects.requireNonNull(amount, "Amount string must not be null");
    Objects.requireNonNull(currencyCode, "Currency code must not be null");

    Currency curr = Currency.getInstance(currencyCode.toUpperCase());
    BigDecimal decimal = new BigDecimal(amount);
    int fractionDigits = Math.max(0, curr.getDefaultFractionDigits());
    long minor = decimal.movePointRight(fractionDigits)
        .setScale(0, RoundingMode.UNNECESSARY)
        .longValueExact();

    return new Money(minor, curr);
  }

  public static Money of(String amount, Currency currency) {
    Objects.requireNonNull(currency, "Currency must not be null");
    return of(amount, currency.getCurrencyCode());
  }

  public static Money ofString(String amount, Currency currency) {
    return of(amount, currency);
  }

  public static Money ofMajor(BigDecimal amount, Currency currency) {
    Objects.requireNonNull(amount, "Amount BigDecimal must not be null");
    Objects.requireNonNull(currency, "Currency must not be null");
    int fractionDigits = Math.max(0, currency.getDefaultFractionDigits());
    long minor = amount.movePointRight(fractionDigits)
        .setScale(0, RoundingMode.UNNECESSARY)
        .longValueExact();
    return new Money(minor, currency);
  }

  public static Money ofMinor(long amountMinor, Currency currency) {
    return new Money(amountMinor, currency);
  }

  public static Money brl(String amount) {
    return of(amount, "BRL");
  }

  public static Money brl(long amountMinor) {
    return new Money(amountMinor, BRL);
  }

  public static Money zero(Currency currency) {
    return new Money(0L, currency);
  }

  public Money plus(Money other) {
    validateSameCurrency(other);
    return new Money(Math.addExact(this.amountMinor, other.amountMinor), this.currency);
  }

  public Money minus(Money other) {
    validateSameCurrency(other);
    long result = Math.subtractExact(this.amountMinor, other.amountMinor);
    if (result < 0) {
      throw new IllegalArgumentException("Subtraction resulted in negative amount: " + result);
    }
    return new Money(result, this.currency);
  }

  public Money multiply(BigDecimal factor, RoundingMode roundingMode) {
    Objects.requireNonNull(factor, "Factor must not be null");
    Objects.requireNonNull(roundingMode, "RoundingMode must not be null");

    BigDecimal currentMinor = BigDecimal.valueOf(this.amountMinor);
    long newMinor = currentMinor.multiply(factor)
        .setScale(0, roundingMode)
        .longValueExact();

    return new Money(newMinor, this.currency);
  }

  public BigDecimal toBigDecimal() {
    int fractionDigits = Math.max(0, currency.getDefaultFractionDigits());
    return BigDecimal.valueOf(amountMinor).movePointLeft(fractionDigits);
  }

  public String toDecimalString() {
    int fractionDigits = Math.max(0, currency.getDefaultFractionDigits());
    return toBigDecimal().setScale(fractionDigits, RoundingMode.UNNECESSARY).toPlainString();
  }

  public String toFormattedString() {
    return toDecimalString();
  }

  public boolean isZero() {
    return this.amountMinor == 0L;
  }

  public boolean isPositive() {
    return this.amountMinor > 0L;
  }

  public boolean isLessThan(Money other) {
    return compareTo(other) < 0;
  }

  public boolean isGreaterThan(Money other) {
    return compareTo(other) > 0;
  }

  private void validateSameCurrency(Money other) {
    Objects.requireNonNull(other, "Other money must not be null");
    if (!this.currency.equals(other.currency)) {
      throw new CurrencyMismatchException(this.currency, other.currency);
    }
  }

  @Override
  public int compareTo(Money other) {
    validateSameCurrency(other);
    return Long.compare(this.amountMinor, other.amountMinor);
  }

  @Override
  public String toString() {
    return toDecimalString() + " " + currency.getCurrencyCode();
  }

  public static class MoneySerializer extends JsonSerializer<Money> {
    @Override
    public void serialize(Money value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      if (value == null) {
        gen.writeNull();
        return;
      }
      gen.writeStartObject();
      gen.writeStringField("amount", value.toDecimalString());
      gen.writeStringField("currency", value.currency().getCurrencyCode());
      gen.writeEndObject();
    }
  }

  public static class MoneyDeserializer extends JsonDeserializer<Money> {
    @Override
    public Money deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      JsonNode node = p.getCodec().readTree(p);
      if (node == null || node.isNull()) {
        return null;
      }
      if (!node.isObject()) {
        throw new IllegalArgumentException(
            "Money JSON representation must be an object containing 'amount' and 'currency'");
      }
      JsonNode amountNode = node.get("amount");
      JsonNode currencyNode = node.get("currency");
      if (amountNode == null || amountNode.isNull() || amountNode.asText().isBlank()
          || currencyNode == null || currencyNode.isNull() || currencyNode.asText().isBlank()) {
        throw new IllegalArgumentException(
            "Money JSON representation must contain 'amount' and 'currency'");
      }
      return Money.of(amountNode.asText(), currencyNode.asText());
    }
  }
}
