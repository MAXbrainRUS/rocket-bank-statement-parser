package ru.maxbrainrus.parser;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import lombok.extern.slf4j.Slf4j;
import ru.maxbrainrus.migrate.AmountWithCcy;
import ru.maxbrainrus.migrate.Amounts;
import ru.maxbrainrus.migrate.MoneyTransaction;
import ru.maxbrainrus.migrate.OperationType;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Slf4j
public class RocketPdfParser {
    public static final String РОКЕТ_WALLET = "Рокет карта";
    private final KeyWordCategoryFiller categoryFiller;
    public static final Pattern AMOUNT_PATTERN = Pattern.compile(" (-?\\d+[ \\d]*([\\.,]\\d+)?) RUR");
    // Pattern that describes start of transaction's description
    private static final Pattern PATTERN_START = Pattern.compile("((\\d{2}\\.\\d{2}\\.\\d{4})|( P2P)? ?([А-ЯЁ][ а-яё]+)).*RUR.*");

    public RocketPdfParser(KeyWordCategoryFiller categoryFiller) {
        this.categoryFiller = categoryFiller;
    }

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
                .filter(s -> !s.startsWith("Руководитель отдела"))
                .filter(s -> !s.startsWith("Специалист"))
//                .filter(s -> !s.matches("([А-ЯЁ][А-ЯЁа-яё]* ?){2,3}")) // ФИО руководителя отдела или специалиста
//                .filter(s -> !s.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) // date at the end of document
//                .filter(s -> !s.matches("№ [\\d\\-/A-Za-z]+"))
                .collect(Collectors.toList()); // Id of document
    }

    private static List<String> filterEndOfDocument(List<String> lines) {
        int indexTailLine = getIndexTailLine(lines);
        if (indexTailLine == -1) {
            return lines;
        }
        return lines.subList(0, indexTailLine);
    }

    private static int getIndexTailLine(List<String> input) {
        for (int i = 0; i < input.size(); i++) {
            String s = input.get(i);
            if (s != null && s.startsWith("Руководитель отдела")) {
                return i;
            }
        }
        return -1;
    }

    private List<MoneyTransaction> parseTextOfPage(String textFromPage) {
        List<String> linesFromPage = Arrays.asList(textFromPage.split("\n"));
        log.info("Got lines from page:\n<START OF PAGE>\n{}\n <END OF PAGE>", String.join("\n", linesFromPage));
        List<String> transactionTexts = collectTransactionTexts(filterNonTransactionLines(linesFromPage));
        return transactionTexts.stream()
                .peek(s -> log.info("Processing transaction text: {}", s))
                .map(transactionText -> new SourceDest(transactionText, MoneyTransaction.builder()))
                .map(this::cutDateToRow)
                .map(this::cutEconomicToRow)
                .map(this::cutOperationDateToRow)
                .map(this::cutGarbage)
                .map(this::cutAllAsDescriptionToRow)
                .map(sourceDest -> sourceDest.getTransactionData().build())
                .map(categoryFiller::fillCategory)
                .peek(moneyTransaction -> log.info("Parsed transaction: {}", moneyTransaction))
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

    private SourceDest cutGarbage(SourceDest sourceDest) {
        String s = sourceDest.getTransactionText();
        s = s
                .replaceAll(" {2,}", " ") // extra spaces after deletions
                .replaceAll(", ?на сумму: -?\\d+[ \\d]*([\\.,]\\d+)?\\([\\dRUB]{1,3}\\)", "") // amount repeated in description
                .replaceAll(", ?карта \\d+[\\*x]+\\d+", "") // card number
                .trim();
        return new SourceDest(s, sourceDest.getTransactionData());
    }

    private SourceDest cutAllAsDescriptionToRow(SourceDest sourceDest) {
        String s = sourceDest.getTransactionText();
        MoneyTransaction.MoneyTransactionBuilder builder = sourceDest.getTransactionData();
        builder.description(s);
        return new SourceDest(s, builder);
    }

    private SourceDest cutOperationDateToRow(SourceDest sourceDest) {
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
    private List<String> collectTransactionTexts(List<String> lines) {
        List<String> accumulator = new ArrayList<>();
        /*
         * If line is a beginning of transaction just adds it to the end of accumulator,
         * otherwise concat them to last element in accumulator.
         * Not 'collect' because algorithm not stateless and depends on order of lines from page
         */
        lines.forEach(line -> {
            try {
                if (PATTERN_START.matcher(line).matches()) {
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

    private void setRoketWalletToTransaction(MoneyTransaction.MoneyTransactionBuilder builder, BigDecimal amount, OperationType operationType) {
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

    private OperationType getOperationType(BigDecimal amount, String transactionText) {
        if (transactionText.contains("перевод") || transactionText.contains("Перевод")) {
            return OperationType.TRANSFER;
        } else if (amount.signum() > 0) {
            return OperationType.INCOME;
        } else {
            return OperationType.EXPENDITURE;
        }
    }

    private void setAmount(BigDecimal amount, MoneyTransaction.MoneyTransactionBuilder builder) {
        builder.amounts(Amounts.builder().sourceAmount(AmountWithCcy.builder().amount(amount.abs()).build()).build());
    }

    private SourceDest cutDateToRow(SourceDest sourceDest) {
        String s = sourceDest.getTransactionText();
        MoneyTransaction.MoneyTransactionBuilder builder = sourceDest.getTransactionData();
        Matcher dateMatcher = Pattern.compile("^\\d{2}\\.\\d{2}\\.\\d{4}").matcher(s);
        if (dateMatcher.find()) {
            builder.date(LocalDate.parse(dateMatcher.group(), DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            s = excludeStringPart(s, dateMatcher);
        }
        return new SourceDest(s, builder);
    }

    private SourceDest cutEconomicToRow(SourceDest sourceDest) {
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

    public List<MoneyTransaction> parsePdf(String fileName) {
        try (AutoClosablePdfReader reader = new AutoClosablePdfReader(fileName)) {
            int numberOfPages = reader.getNumberOfPages();
            List<MoneyTransaction> res = new ArrayList<>();
            for (int i = 1; i <= numberOfPages; i++) {
                String textFromPage = PdfTextExtractor.getTextFromPage(reader, i);
                // transaction can't be separated on several pdf pages
                List<MoneyTransaction> transactions = parseTextOfPage(textFromPage);
                res.addAll(transactions);
            }
            res = postEditing(res);
            return res;
        } catch (IOException e) {
            log.error("An error occurred while read pdf source report.");
            throw new RuntimeException(e);
        }
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

    private List<MoneyTransaction> postEditing(List<MoneyTransaction> transactions) {
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

class AutoClosablePdfReader extends PdfReader implements AutoCloseable {

    public AutoClosablePdfReader(String filename) throws IOException {
        super(filename);
    }
}