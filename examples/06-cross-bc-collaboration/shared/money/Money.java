package com.example.booking.shared.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * A money value object — amount plus currency, scaled to the currency's
 * fraction digits. A shared-kernel type: it lives in
 * {@code com.example.booking.shared.money} and is reused by any context that
 * deals in prices, so a {@code PlanDto} can carry a {@code Money} without the
 * plan and subscription contexts inventing two different "amount + currency"
 * shapes.
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "cannot combine different currencies: %s vs %s".formatted(currency, other.currency));
        }
    }
}
