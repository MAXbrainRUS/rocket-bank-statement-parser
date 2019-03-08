package ru.maxbrainrus.migrate;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
public class MoneyTransaction {
    OperationType operationType;
    LocalDate date;
    String description;
    Amounts amounts;
    String category;
    String sourceWallet;
    String targetWallet; // only for transfer operation type
    /**
     * Temporal field.
     * Date of operation in description of source pdf.
     */
    LocalDateTime operationDate;
}
