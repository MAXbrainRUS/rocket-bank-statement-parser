package ru.maxbrainrus.parser;

import lombok.extern.slf4j.Slf4j;
import ru.maxbrainrus.transaction.AmountWithCcy;
import ru.maxbrainrus.transaction.Amounts;
import ru.maxbrainrus.transaction.MoneyTransaction;
import ru.maxbrainrus.transaction.OperationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static ru.maxbrainrus.parser.PdfTextExtractor.getTextFromPdf;

@Slf4j
public class RocketPdfParser {
    public static final String РОКЕТ_WALLET = "Рокет карта";
    public static final Pattern START_OF_DOCUMENT_TAIL_PATTERN = Pattern.compile("^(Руководитель отдела|Специалист)");
    public static final Pattern AMOUNT_PATTERN = Pattern.compile(" (-?\\d+[ \\d]*([\\.,]\\d+)?) RUR");
    // Pattern that describes start of transaction's description
    private static final Pattern START_TRANSACTION_LINE_PATTERN = Pattern.compile("((\\d{2}\\.\\d{2}\\.\\d{4})|( P2P)? ?([А-ЯЁ][ а-яё]+)).*RUR.*");

    private static List<String> filterNonTransactionLines(List<String> input) {
        return filterEndOfDocument(input).stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .filter(s -> !s.startsWith("Выписка"))
                .filter(s -> !s.startsWith("ФИО:"))
                .filter(s -> !s.startsWith("Адрес регистрации"))
                .filter(s -> !s.startsWith("Счет No"))
                .filter(s -> !s.startsWith("Период:"))
                .filter(s -> !s.startsWith("Входящий остаток:"))
                .filter(s -> !s.startsWith("Исходящий остаток:"))
                .filter(s -> !s.startsWith("Дата Описание Расход"))
                .filter(s -> !s.startsWith("Итог:"))
                .collect(Collectors.toList()); // Id of document
    }

    private static List<String> filterEndOfDocument(List<String> lines) {
        int indexTailLine = getIndexTailLine(lines);
        if (indexTailLine == -1) {
            log.warn("End of document is not found. Faked lines can be processed");
            return lines;
        }
        return lines.subList(0, indexTailLine);
    }

    private static int getIndexTailLine(List<String> input) {
        for (int i = 0; i < input.size(); i++) {
            String s = input.get(i);
            if (s != null && START_OF_DOCUMENT_TAIL_PATTERN.matcher(s).find()) {
                return i;
            }
        }
        return -1;
    }

    private static List<MoneyTransaction> parseTexts(List<String> documentTextsPerPage) {
        List<String> linesFromDocument = splitByLines(documentTextsPerPage);
        List<String> transactionTexts = collectTransactionTexts(filterNonTransactionLines(linesFromDocument));
        return transactionTexts.stream()
                .peek(s -> log.info("Got transaction text: {}", s))
                .map(transactionText -> new SourceDest(transactionText, MoneyTransaction.builder()))
                .map(RocketPdfParser::cutDateToRow)
                .map(RocketPdfParser::cutEconomicToRow)
                .map(RocketPdfParser::cutOperationDateToRow)
                .map(RocketPdfParser::cutGarbage)
                .map(RocketPdfParser::cutAllAsDescriptionToRow)
                .map(sourceDest -> sourceDest.getTransactionData().build())
                .collect(Collectors.toList());
    }

    private static List<String> splitByLines(List<String> documentTextsPerPage) {
        return documentTextsPerPage.stream()
                .flatMap(textFromPage -> {
                    List<String> linesFromPage = Arrays.asList(textFromPage.split("\n"));
                    log.info("Got lines from page:\n<START OF PAGE>\n{}\n <END OF PAGE>", String.join("\n", linesFromPage));
                    return linesFromPage.stream();
                })
                .collect(Collectors.toList());
    }

    private static String cutAmounts(String transactionText, List<BigDecimal> resultDest) {
        for (Matcher matcher = AMOUNT_PATTERN.matcher(transactionText); matcher.find(); matcher = AMOUNT_PATTERN.matcher(transactionText)) {
            String amountStringValue = matcher.group(1)
                    .replace(" ", "")
                    .replace(",", ".");
            BigDecimal amount = new BigDecimal(amountStringValue);
            resultDest.add(amount);
            transactionText = excludeStringPart(transactionText, matcher);
        }
        return transactionText;
    }

    private static SourceDest cutGarbage(SourceDest sourceDest) {
        String s = sourceDest.getTransactionText();
        s = s
                .replaceAll(" {2,}", " ") // extra spaces after deletions
                .replaceAll(", ?на сумму: -?\\d+[ \\d]*([\\.,]\\d+)?\\([\\dRUB]{1,3}\\)", "") // amount repeated in description
                .replaceAll(", ?карта \\d+[\\*x]+\\d+", "") // card number
                .trim();
        return new SourceDest(s, sourceDest.getTransactionData());
    }

    private static SourceDest cutAllAsDescriptionToRow(SourceDest sourceDest) {
        String s = sourceDest.getTransactionText();
        MoneyTransaction.MoneyTransactionBuilder builder = sourceDest.getTransactionData();
        builder.description(s);
        return new SourceDest(s, builder);
    }

    private static SourceDest cutOperationDateToRow(SourceDest sourceDest) {
        String s = sourceDest.getTransactionText();
        MoneyTransaction.MoneyTransactionBuilder builder = sourceDest.getTransactionData();
        Matcher operationDateMatcher = Pattern.compile(", ?дата +операции: ?(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2})").matcher(s);
        if (operationDateMatcher.find()) {
            builder.operationDate(LocalDateTime.parse(operationDateMatcher.group(1), DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            s = excludeStringPart(s, operationDateMatcher);
        }
        return new SourceDest(s, sourceDest.getTransactionData());
    }

    private static String excludeStringPart(String s, Matcher matcher) {
        return s.substring(0, matcher.start()) + s.substring(matcher.end());
    }

    /**
     * Collect lines to texts of transactions
     *
     * @param lines string lines from page
     * @return list of texts where each text refers to one transaction
     */
    private static List<String> collectTransactionTexts(List<String> lines) {
        List<String> accumulator = new ArrayList<>();
        /*
         * If line is a beginning of transaction just adds it to the end of accumulator,
         * otherwise concat them to last element in accumulator.
         * Not 'collect' because algorithm not stateless and depends on order of lines from page
         */
        lines.forEach(line -> {
            try {
                if (START_TRANSACTION_LINE_PATTERN.matcher(line).matches()) {
                    accumulator.add(line);
                } else {
                    int lastIndex = accumulator.size() - 1;
                    accumulator.set(lastIndex, accumulator.get(lastIndex) + " " + line);
                }
            } catch (RuntimeException e) {
                log.error("Error with line '{}'", line);
                throw e;
            }
        });
        return accumulator;
    }

    private static void setRoketWalletToTransaction(MoneyTransaction.MoneyTransactionBuilder builder, BigDecimal amount, OperationType operationType) {
        if (operationType == OperationType.TRANSFER) {
            if (amount.signum() > 0) {
                builder.targetWallet(РОКЕТ_WALLET);
            } else {
                builder.sourceWallet(РОКЕТ_WALLET);
            }
        } else {
            builder.sourceWallet(РОКЕТ_WALLET);
        }
    }

    private static OperationType getOperationType(BigDecimal amount, String transactionText) {
        if (transactionText.contains("перевод") || transactionText.contains("Перевод")) {
            return OperationType.TRANSFER;
        } else if (amount.signum() > 0) {
            return OperationType.INCOME;
        } else {
            return OperationType.EXPENDITURE;
        }
    }

    private static void setAmount(BigDecimal amount, MoneyTransaction.MoneyTransactionBuilder builder) {
        builder.amounts(Amounts.builder().sourceAmount(AmountWithCcy.builder().amount(amount.abs()).build()).build());
    }

    private static SourceDest cutDateToRow(SourceDest sourceDest) {
        String s = sourceDest.getTransactionText();
        MoneyTransaction.MoneyTransactionBuilder builder = sourceDest.getTransactionData();
        Matcher dateMatcher = Pattern.compile("^\\d{2}\\.\\d{2}\\.\\d{4}").matcher(s);
        if (dateMatcher.find()) {
            builder.date(LocalDate.parse(dateMatcher.group(), DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            s = excludeStringPart(s, dateMatcher);
        }
        return new SourceDest(s, builder);
    }

    private static SourceDest cutEconomicToRow(SourceDest sourceDest) {
        String transactionText = sourceDest.getTransactionText();
        MoneyTransaction.MoneyTransactionBuilder builder = sourceDest.getTransactionData();
        List<BigDecimal> amounts = new ArrayList<>();
        transactionText = cutAmounts(transactionText, amounts);
        BigDecimal amount = amounts.get(amounts.size() - 1);

        OperationType operationType = getOperationType(amount, transactionText);
        setRoketWalletToTransaction(builder, amount, operationType);
        builder.operationType(operationType);
        setAmount(amount, builder);

        return new SourceDest(transactionText, builder);
    }

    public static List<MoneyTransaction> parsePdf(String fileName) {
        List<String> textsPerPageFromPdf = getTextFromPdf(fileName);
        List<MoneyTransaction> transactions = parseTexts(textsPerPageFromPdf);
        return postEditing(transactions);
    }


    private static MoneyTransaction rewriteDateWithOperationDate(MoneyTransaction transaction) {
        LocalDateTime operationDate = transaction.getOperationDate();
        if (operationDate != null) {
            return transaction.toBuilder().date(operationDate.toLocalDate()).build();
        }
        return transaction;
    }

    private static List<MoneyTransaction> fillDatesFromTransactionsAbove(List<MoneyTransaction> transactions) {
        List<MoneyTransaction> res = new ArrayList<>(transactions);
        for (int i = 1; i < res.size(); i++) {
            if (res.get(i).getDate() == null) {
                res.set(i, res.get(i).toBuilder().date(res.get(i - 1).getDate()).build());
            }
        }
        return res;
    }

    private static List<MoneyTransaction> postEditing(List<MoneyTransaction> transactions) {
        // If transactions are in one day, only first transaction will be with filled date.
        // Fill dates from upper transactions
        List<MoneyTransaction> res = fillDatesFromTransactionsAbove(transactions);
        // If transaction date has operation date it will be more suit.
        // Transactions from shops has date when money income to bank. It happens after few days after operation date.
        return res.stream()
                .filter(moneyTransaction -> !isEmpty(moneyTransaction.getDescription())) // fake transaction with date only should be removed
                .map(RocketPdfParser::rewriteDateWithOperationDate)
                .sorted(Comparator.comparing(MoneyTransaction::getDate))
                .collect(Collectors.toList());
    }
}

class SourceDest {
    private String transactionText;
    private MoneyTransaction.MoneyTransactionBuilder transactionData;

    public SourceDest(String transactionText, MoneyTransaction.MoneyTransactionBuilder transactionData) {
        this.transactionText = transactionText;
        this.transactionData = transactionData;
    }

    public String getTransactionText() {
        return transactionText;
    }

    public MoneyTransaction.MoneyTransactionBuilder getTransactionData() {
        return transactionData;
    }

    @Override
    public String toString() {
        return "SourceDest{" +
                "transactionText='" + transactionText + '\'' +
                ", transactionData=" + transactionData +
                '}';
    }
}