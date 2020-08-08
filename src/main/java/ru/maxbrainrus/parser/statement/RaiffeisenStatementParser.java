package ru.maxbrainrus.parser.statement;

import org.apache.commons.csv.CSVRecord;
import ru.maxbrainrus.parser.StatementParser;
import ru.maxbrainrus.transaction.AmountWithCcy;
import ru.maxbrainrus.transaction.Amounts;
import ru.maxbrainrus.transaction.MoneyTransaction;
import ru.maxbrainrus.transaction.OperationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RaiffeisenStatementParser extends CsvStatementParser implements StatementParser {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Override
    protected MoneyTransaction readTransaction(CSVRecord record, String sourceWallet) {
        BigDecimal amount = readAmount(record);
        String description = getDescription(record);
        OperationType operationType = getOperationType(amount, description);

        MoneyTransaction.MoneyTransactionBuilder moneyTransactionBuilder = MoneyTransaction.builder()
                .date(getOperationDate(record))
                .amounts(getAmount(amount.abs()))
                .description(description)
                .operationType(operationType);

        setWalletValue(moneyTransactionBuilder, sourceWallet, amount, operationType);

        return moneyTransactionBuilder
                .build();
    }

    private static void setWalletValue(MoneyTransaction.MoneyTransactionBuilder builder, String sourceWallet, BigDecimal amount, OperationType operationType) {
        if (operationType == OperationType.TRANSFER) {
            if (amount.signum() > 0) {
                builder.targetWallet(sourceWallet);
            } else {
                builder.sourceWallet(sourceWallet);
            }
        } else {
            builder.sourceWallet(sourceWallet);
        }
    }

    private static OperationType getOperationType(BigDecimal amount, String transactionText) {
        if (transactionText.contains("перевод") ||
                transactionText.contains("Перевод") ||
                transactionText.contains("ATM ")) {
            return OperationType.TRANSFER;
        } else if (amount.signum() > 0) {
            return OperationType.INCOME;
        } else {
            return OperationType.EXPENDITURE;
        }
    }

    private String getDescription(CSVRecord record) {
        return record.get("Описание");
    }

    private Amounts getAmount(BigDecimal amount) {
        return Amounts.builder()
                .sourceAmount(
                        AmountWithCcy.builder()
                                .amount(amount)
                                .build())
                .build();
    }

    private BigDecimal readAmount(CSVRecord record) {
        String amountValue = record.get("Сумма в валюте счета");
        amountValue = amountValue.replaceAll(" ", "");
        return new BigDecimal(amountValue);
    }

    private LocalDate getOperationDate(CSVRecord record) {
        String operationDateValue = readOperationDate(record);
        return LocalDateTime.parse(operationDateValue, DATE_TIME_FORMATTER).toLocalDate();
    }

    private String readOperationDate(CSVRecord record) {
        if (record.isMapped("Дата операции")) {
            return record.get("Дата операции");
        } else if (record.isMapped("Дата транзакции")) {
            return record.get("Дата транзакции");
        } else {
            throw new IllegalArgumentException();
        }
    }
}
