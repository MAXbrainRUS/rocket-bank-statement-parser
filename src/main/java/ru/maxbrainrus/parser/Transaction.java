package ru.maxbrainrus.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class Transaction {
    /**
     * Date of operation.
     * The same as operation date, if it has is source pdf.
     */
    private LocalDate date;
    /**
     * Some additional data of transaction.
     */
    private String description;
    /**
     * Temporal field.
     * Date of operation in description of source pdf.
     */
    private LocalDateTime operationDate;
    private BigDecimal amountArrival;
    private BigDecimal amountExpenditure;
    private String category;
}
