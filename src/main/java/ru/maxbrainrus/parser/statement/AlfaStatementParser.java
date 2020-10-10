package ru.maxbrainrus.parser.statement;

import org.apache.commons.csv.CSVRecord;
import ru.maxbrainrus.transaction.AmountWithCcy;
import ru.maxbrainrus.transaction.Amounts;
import ru.maxbrainrus.transaction.MoneyTransaction;
import ru.maxbrainrus.transaction.OperationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class AlfaStatementParser extends CsvStatementParser implements BankStatementParser {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yy");
    private static final Pattern DATE_REGEX_PATTERN = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{2}");
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("\\d{4,}\\++\\d{4}");

    private static String splitAndGetLast(String input, String regex) {
        String[] strings = input.split(regex);
        return strings[strings.length - 1];
    }

    private static String splitAndGetFirst(String input, String regex) {
        return input.split(regex)[0];
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
        if (transactionText.contains("CARD2CARD") ||
                transactionText.contains("Внутрибанковский перевод между счетами")) {
            return OperationType.TRANSFER;
        } else if (amount.signum() > 0) {
            return OperationType.INCOME;
        } else {
            return OperationType.EXPENDITURE;
        }
    }

    private static LocalDate convertToDate(String dateString) {
        return DATE_TIME_FORMATTER.parse(dateString, LocalDate::from);
    }

    @Override
    protected MoneyTransaction readTransaction(CSVRecord record, String sourceWallet) {
        BigDecimal amount = readAmount(record);
        String rawDescription = getRawDescription(record);
        OperationType operationType = getOperationType(amount, rawDescription);

        MoneyTransaction.MoneyTransactionBuilder moneyTransactionBuilder = MoneyTransaction.builder()
                .date(getOperationDate(rawDescription, record))
                .amounts(convertToAmounts(amount.abs()))
                .description(getUsefulDescription(rawDescription))
                .operationType(operationType);

        setWalletValue(moneyTransactionBuilder, sourceWallet, amount, operationType);

        return moneyTransactionBuilder.build();
    }

    private String getUsefulDescription(String rawDescription) {
        if (CARD_NUMBER_PATTERN.matcher(rawDescription).find()) {
            return Optional.of(rawDescription)
                    .map(s -> splitAndGetFirst(s, ">"))
                    .map(s -> splitAndGetLast(s, "/"))
                    .map(s -> splitAndGetLast(s, "\\\\"))
                    .map(s -> splitAndGetFirst(s, "  "))
                    .map(String::trim)
                    .get();
        }
        return rawDescription;
    }

    private String getRawDescription(CSVRecord record) {
        return record.get("Описание операции");
    }

    private Amounts convertToAmounts(BigDecimal amount) {
        return Amounts.builder()
                .sourceAmount(
                        AmountWithCcy.builder()
                                .amount(amount)
                                .build())
                .build();
    }

    private BigDecimal readAmount(CSVRecord record) {
        return Stream.of(
                getAmount(record, "Расход", true),
                getAmount(record, "Приход", false)
        )
                .filter(Objects::nonNull)
                .filter(bigDecimal -> !BigDecimal.ZERO.equals(bigDecimal))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find correct amount"));
    }

    private BigDecimal getAmount(CSVRecord record, String fieldName, boolean invertSign) {
        if (record.isMapped(fieldName)) {
            String amountStringValue = record.get(fieldName).replaceAll(",", ".");
            BigDecimal amountValue = new BigDecimal(amountStringValue);
            return invertSign ? amountValue.negate() : amountValue;
        }
        return null;
    }

    private LocalDate getOperationDate(String rawDescription, CSVRecord record) {
        return getOperationDateFromDescription(rawDescription)
                .orElseGet(() -> convertToDate(record.get("Дата операции")));
    }

    private Optional<LocalDate> getOperationDateFromDescription(String rawDescription) {
        Matcher matcher = DATE_REGEX_PATTERN.matcher(rawDescription);
        List<String> datesInDescription = new ArrayList<>();
        while (matcher.find()) {
            String dateInDescription = matcher.group();
            datesInDescription.add(dateInDescription);
        }
        return datesInDescription.stream()
                .map(AlfaStatementParser::convertToDate)
                .sorted()
                .findFirst();
    }
}
