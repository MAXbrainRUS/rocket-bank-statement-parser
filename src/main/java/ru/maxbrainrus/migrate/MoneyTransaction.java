package ru.maxbrainrus.migrate;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class MoneyTransaction {
    OperationType operationType;
    LocalDate date;
    String description;
    Amounts amounts;
    String category;
    String sourceWallet;
    String targetWallet; // only for transfer operation type
}
