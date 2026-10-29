package com.ecommerce.shared.error;

import java.util.Currency;

public class CurrencyMismatchException extends DomainException {

  public CurrencyMismatchException(Currency expected, Currency actual) {
    super(
        FailureCode.CURRENCY_MISMATCH,
        "Cannot operate on money with different currencies: "
            + expected.getCurrencyCode()
            + " vs "
            + actual.getCurrencyCode());
  }
}
