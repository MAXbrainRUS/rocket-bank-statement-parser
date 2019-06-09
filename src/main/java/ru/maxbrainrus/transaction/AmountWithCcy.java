package ru.maxbrainrus.transaction;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class AmountWithCcy {
    BigDecimal amount;
    String ccy;
}
