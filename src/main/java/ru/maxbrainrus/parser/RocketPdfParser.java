package ru.maxbrainrus.parser;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RocketPdfParser {
    private final KeyWordCategoryFiller categoryFiller;
    // Pattern that describes start of transaction's description
    private Pattern patternStart = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})?( P2P)? ?([А-ЯЁ][ а-яё]+).*RUR.*");

    public RocketPdfParser(KeyWordCategoryFiller categoryFiller) {
        this.categoryFiller = categoryFiller;
    }

    private static Stream<String> filterNonTransactionLines(Stream<String> input) {
        return input
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
                .filter(s -> !s.matches("([А-ЯЁ][А-ЯЁа-яё]* ?){2,3}")) // ФИО руководителя отдела или специалиста
                .filter(s -> !s.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) // date at the end of document
                .filter(s -> !s.matches("№ [\\d\\-/A-Za-z]+")); // Id of document
    }

    private List<Transaction> parseTextOfPage(String textFromPage) {
        String[] linesFromPage = textFromPage.split("\n");
        List<String> transactionTexts = collectTransactionTexts(filterNonTransactionLines(Arrays.stream(linesFromPage)));
        return transactionTexts.stream()
                .peek(System.out::println)
                .map(transactionText -> new SourceDest(transactionText, Transaction.builder()))
                .map(this::cutDateToRow)
                .map(this::cutAmountToRow)
                .map(this::cutOperationDateToRow)
                .map(this::cutGarbage)
                .map(this::cutAllAsDescriptionToRow)
                .map(sourceDest -> sourceDest.getTransactionData().build())
                .map(categoryFiller::fillCategory)
                .peek(System.out::println)
                .collect(Collectors.toList());
    }

    /**
     * Collect lines to texts of transactions
     *
     * @param lines string lines from page
     * @return list of texts where each text refers to one transaction
     */
    private List<String> collectTransactionTexts(Stream<String> lines) {
        List<String> accumulator = new ArrayList<>();
        /*
         * If line is a beginning of transaction just adds it to the end of accumulator,
         * otherwise concat them to last element in accumulator.
         * Not 'collect' because algorithm not stateless and depends on order of lines from page
         */
        lines.forEachOrdered(line -> {
            if (patternStart.matcher(line).matches()) {
                accumulator.add(line);
            } else {
                int lastIndex = accumulator.size() - 1;
                assert lastIndex != -1;
                accumulator.set(lastIndex, accumulator.get(lastIndex) + " " + line);
            }
        });
        return accumulator;
    }

    private SourceDest cutGarbage(SourceDest sourceDest) {
        String s = sourceDest.getTransactionText();
        Transaction.TransactionBuilder builder = sourceDest.getTransactionData();
        s = s
                .replaceAll(" {2,}", " ") // extra spaces after deletions
                .replaceAll(", ?на сумму: -?\\d+[ \\d]*(\\.\\d+)?\\([\\dRUB]{1,3}\\)", "") // amount repeated in description
                .replaceAll(", ?карта \\d+[\\*x]+\\d+", "") // card number
                .trim();
        return new SourceDest(s, builder);
    }

    private SourceDest cutAllAsDescriptionToRow(SourceDest sourceDest) {
        String s = sourceDest.getTransactionText();
        Transaction.TransactionBuilder builder = sourceDest.getTransactionData();
        builder.description(s);
        return new SourceDest(s, builder);
    }

    private SourceDest cutOperationDateToRow(SourceDest sourceDest) {
        String s = sourceDest.getTransactionText();
        Transaction.TransactionBuilder builder = sourceDest.getTransactionData();
        Matcher operationDateMatcher = Pattern.compile(", ?дата +операции: ?(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2})").matcher(s);
        if (operationDateMatcher.find()) {
            builder.operationDate(LocalDateTime.parse(operationDateMatcher.group(1), DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            s = excludeStringPart(s, operationDateMatcher);
        }
        return new SourceDest(s, builder);
    }

    private SourceDest cutAmountToRow(SourceDest sourceDest) {
        String s = sourceDest.getTransactionText();
        Transaction.TransactionBuilder builder = sourceDest.getTransactionData();
        Pattern amountPattern = Pattern.compile(" (-?\\d+[ \\d]*(\\.\\d+)?) RUR");
        Matcher amountMatcher = amountPattern.matcher(s);
        if (amountMatcher.find()) {
            String amountStringValue = amountMatcher.group(1).replace(" ", "");
            BigDecimal amount = new BigDecimal(amountStringValue);
            if (amount.signum() > 0) {
                builder.amountArrival(amount);
            } else {
                builder.amountExpenditure(amount.abs());
            }
            s = excludeStringPart(s, amountMatcher);
        }

        // Transaction may contain subtotal of account (has the same pattern)
        // Exclude them from transaction text
        for (Matcher matcher = amountPattern.matcher(s); matcher.find(); matcher = amountPattern.matcher(s)) {
            s = excludeStringPart(s, matcher);
        }
        return new SourceDest(s, builder);
    }

    private SourceDest cutDateToRow(SourceDest sourceDest) {
        String s = sourceDest.getTransactionText();
        Transaction.TransactionBuilder builder = sourceDest.getTransactionData();
        Matcher dateMatcher = Pattern.compile("^\\d{2}\\.\\d{2}\\.\\d{4}").matcher(s);
        if (dateMatcher.find()) {
            builder.date(LocalDate.parse(dateMatcher.group(), DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            s = excludeStringPart(s, dateMatcher);
        }
        return new SourceDest(s, builder);
    }

    private String excludeStringPart(String s, Matcher matcher) {
        return s.substring(0, matcher.start()) + s.substring(matcher.end());
    }

    public List<Transaction> parsePdf(String fileName) {
        try (AutoClosablePdfReader reader = new AutoClosablePdfReader(fileName)) {
            int numberOfPages = reader.getNumberOfPages();
            List<Transaction> res = new ArrayList<>();
            for (int i = 1; i <= numberOfPages; i++) {
                String textFromPage = PdfTextExtractor.getTextFromPage(reader, i);
                // transaction can't be separated on several pdf pages
                List<Transaction> transactions = parseTextOfPage(textFromPage);
                res.addAll(transactions);
            }
            postEditing(res);
            return res;
        } catch (IOException e) {
            System.err.println("An error occurred while read pdf source report.");
            throw new RuntimeException(e);
        }
    }

    private void postEditing(List<Transaction> res) {
        // if transactions are in one day, only first transaction will be with filled date.
        // fill dates from upper transactions
        for (int i = 1; i < res.size(); i++) {
            if (res.get(i).getDate() == null) {
                res.set(i, res.get(i).toBuilder().date(res.get(i - 1).getDate()).build());
            }
        }
        // If transaction date has operation date it will be more suit.
        // Transactions from shops has date when money income to bank. It happens after few days after operation date.
        for (int i = 0; i < res.size(); i++) {
            LocalDateTime operationDate = res.get(i).getOperationDate();
            if (operationDate != null) {
                res.set(i, res.get(i).toBuilder().date(operationDate.toLocalDate()).build());
            }
        }
        res.sort(Comparator.comparing(Transaction::getDate));
    }
}

class SourceDest {
    private String transactionText;
    private Transaction.TransactionBuilder transactionData;

    public SourceDest(String transactionText, Transaction.TransactionBuilder transactionData) {
        this.transactionText = transactionText;
        this.transactionData = transactionData;
    }

    public String getTransactionText() {
        return transactionText;
    }

    public Transaction.TransactionBuilder getTransactionData() {
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