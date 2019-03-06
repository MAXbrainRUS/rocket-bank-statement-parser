package ru.maxbrainrus.migrate;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Amounts {
    AmountWithCcy sourceAmount;
    AmountWithCcy targetAmount; // only for transfer operation type
}
