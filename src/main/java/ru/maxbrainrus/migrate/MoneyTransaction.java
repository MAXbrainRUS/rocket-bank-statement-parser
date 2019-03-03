package ru.maxbrainrus.migrate;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class MoneyTransaction {
    OperationType operationType;
    LocalDate date;
    String description;
    BigDecimal amountArrival;
    String ccyArrival;
    BigDecimal amountExpenditure;
    String ccyExpenditure;
    String category;
    String sourceWallet;
    String targetWallet; // only for transfer operation type

}
