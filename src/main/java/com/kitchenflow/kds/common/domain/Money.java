package com.kitchenflow.kds.common.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount) implements Comparable<Money> {
    public Money {
        if (amount == null || amount.scale() != 2) {
            throw new IllegalArgumentException("Money must have exactly two decimal places.");
        }
    }

    public static Money parse(String value) {
        return new Money(new BigDecimal(value));
    }

    public static Money round(BigDecimal value) {
        return new Money(value.setScale(2, RoundingMode.HALF_UP));
    }

    public Money plus(Money other) { return new Money(amount.add(other.amount)); }
    public Money minus(Money other) { return new Money(amount.subtract(other.amount)); }
    public Money times(int quantity) { return round(amount.multiply(BigDecimal.valueOf(quantity))); }
    public Money percent(BigDecimal rate) { return round(amount.multiply(rate).divide(BigDecimal.valueOf(100))); }
    public boolean isNegative() { return amount.signum() < 0; }
    @Override public int compareTo(Money other) { return amount.compareTo(other.amount); }
    @Override public String toString() { return amount.toPlainString(); }
}
