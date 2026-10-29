package com.ecommerce.shared.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecommerce.shared.error.CurrencyMismatchException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyTest {

  private static final Currency BRL = Currency.getInstance("BRL");
  private static final Currency USD = Currency.getInstance("USD");
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("Should create Money from minor units")
  void shouldCreateMoney_whenMinorUnitsProvided() {
    Money money = Money.ofMinor(19990L, BRL);
    assertThat(money.amountMinor()).isEqualTo(19990L);
    assertThat(money.currency()).isEqualTo(BRL);
    assertThat(money.toFormattedString()).isEqualTo("199.90");
    assertThat(money.toDecimalString()).isEqualTo("199.90");
  }

  @Test
  @DisplayName("Should create Money from major units BigDecimal")
  void shouldCreateMoney_whenBigDecimalProvided() {
    Money money = Money.ofMajor(new BigDecimal("199.90"), BRL);
    assertThat(money.amountMinor()).isEqualTo(19990L);
    assertThat(money.currency()).isEqualTo(BRL);
  }

  @Test
  @DisplayName("Should create Money from string")
  void shouldCreateMoney_whenStringAmountProvided() {
    Money money = Money.ofString("199.90", BRL);
    assertThat(money.amountMinor()).isEqualTo(19990L);
    assertThat(money.currency()).isEqualTo(BRL);
  }

  @Test
  @DisplayName("Should create zero Money")
  void shouldCreateZeroMoney_whenZeroRequested() {
    Money money = Money.zero(BRL);
    assertThat(money.amountMinor()).isEqualTo(0L);
    assertThat(money.isZero()).isTrue();
    assertThat(money.isPositive()).isFalse();
  }

  @Test
  @DisplayName("Should reject negative amounts")
  void shouldThrowException_whenAmountIsNegative() {
    assertThatThrownBy(() -> Money.ofMinor(-100L, BRL))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be negative");

    assertThatThrownBy(() -> Money.ofMajor(new BigDecimal("-1.00"), BRL))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should add two money values with same currency")
  void shouldAddAmounts_whenSameCurrency() {
    Money m1 = Money.ofMinor(1000L, BRL);
    Money m2 = Money.ofMinor(550L, BRL);
    Money result = m1.plus(m2);

    assertThat(result.amountMinor()).isEqualTo(1550L);
    assertThat(result.currency()).isEqualTo(BRL);
  }

  @Test
  @DisplayName("Should subtract money values with same currency")
  void shouldSubtractAmounts_whenSameCurrency() {
    Money m1 = Money.ofMinor(1000L, BRL);
    Money m2 = Money.ofMinor(300L, BRL);
    Money result = m1.minus(m2);

    assertThat(result.amountMinor()).isEqualTo(700L);
    assertThat(result.currency()).isEqualTo(BRL);
  }

  @Test
  @DisplayName("Should fail subtraction if result is negative")
  void shouldThrowException_whenSubtractionYieldsNegative() {
    Money m1 = Money.ofMinor(100L, BRL);
    Money m2 = Money.ofMinor(200L, BRL);

    assertThatThrownBy(() -> m1.minus(m2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Subtraction resulted in negative amount");
  }

  @Test
  @DisplayName("Should throw CurrencyMismatchException when currencies differ")
  void shouldThrowCurrencyMismatchException_whenCurrenciesDiffer() {
    Money brl = Money.ofMinor(1000L, BRL);
    Money usd = Money.ofMinor(1000L, USD);

    assertThatThrownBy(() -> brl.plus(usd))
        .isInstanceOf(CurrencyMismatchException.class);

    assertThatThrownBy(() -> brl.minus(usd))
        .isInstanceOf(CurrencyMismatchException.class);
  }

  @Test
  @DisplayName("Should compare money values properly")
  void shouldCompareCorrectly_whenComparingTwoMoneyValues() {
    Money small = Money.ofMinor(100L, BRL);
    Money large = Money.ofMinor(200L, BRL);

    assertThat(small.isLessThan(large)).isTrue();
    assertThat(large.isGreaterThan(small)).isTrue();
    assertThat(small.compareTo(large)).isNegative();
  }

  @Test
  @DisplayName("Should serialize to and deserialize from JSON matching API contract")
  void shouldSerializeAndDeserializeJson_whenContractFormatUsed() throws Exception {
    Money money = Money.of("199.90", "BRL");
    String json = objectMapper.writeValueAsString(money);

    assertThat(json).contains("\"amount\":\"199.90\"");
    assertThat(json).contains("\"currency\":\"BRL\"");
    assertThat(json).doesNotContain("amountMinor");

    Money deserialized = objectMapper.readValue(json, Money.class);
    assertThat(deserialized).isEqualTo(money);
    assertThat(deserialized.amountMinor()).isEqualTo(19990L);
    assertThat(deserialized.currency()).isEqualTo(BRL);
  }
}
