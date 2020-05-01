package ru.maxbrainrus.parser.raiffeisen;

import lombok.SneakyThrows;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import ru.maxbrainrus.parser.StatementParser;
import ru.maxbrainrus.transaction.AmountWithCcy;
import ru.maxbrainrus.transaction.Amounts;
import ru.maxbrainrus.transaction.MoneyTransaction;
import ru.maxbrainrus.transaction.OperationType;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RaiffeisenStatementParser implements StatementParser {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static Charset getCharset1251() {
        return Charset.forName("windows-1251");
    }

    @SneakyThrows
    @Override
    public List<MoneyTransaction> parseBankStatement(String inputDataFileName, String sourceWallet) {
        try (FileInputStream in = new FileInputStream(inputDataFileName)) {
            try (InputStreamReader isr = new InputStreamReader(in, getCharset1251())) {
                try (CSVParser csvParser = getCsvFormat().parse(isr)) {
                    return StreamSupport.stream(csvParser.spliterator(), false)
                            .map(record -> readTransaction(record, sourceWallet))
                            .collect(Collectors.toList());
                }
            }
        }
    }

    private MoneyTransaction readTransaction(CSVRecord record, String sourceWallet) {
        return MoneyTransaction.builder()
                .date(getOperationDate(record))
                .amounts(getAmount(record))
                .sourceWallet(sourceWallet)
                .description(getDescription(record))
                .seller(getSeller(record))
                .operationType(OperationType.EXPENDITURE)
                .build();
    }

    private String getSeller(CSVRecord record) {
        return record.get("Продавец");
    }

    private String getDescription(CSVRecord record) {
        return record.get("Описание");
    }

    private Amounts getAmount(CSVRecord record) {
        BigDecimal amount = readAmount(record);
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
        BigDecimal amount = new BigDecimal(amountValue);
        amount = amount.abs();
        return amount;
    }

    private LocalDate getOperationDate(CSVRecord record) {
        String operationDateValue = record.get("Дата операции");
        return LocalDateTime.parse(operationDateValue, DATE_TIME_FORMATTER).toLocalDate();
    }

    private CSVFormat getCsvFormat() {
        return CSVFormat.DEFAULT
                .withDelimiter(';')
                .withFirstRecordAsHeader();
    }
}
