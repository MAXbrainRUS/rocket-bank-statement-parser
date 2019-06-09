package ru.maxbrainrus.report;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import ru.maxbrainrus.transaction.AmountWithCcy;
import ru.maxbrainrus.transaction.Amounts;
import ru.maxbrainrus.transaction.MoneyTransaction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class CsvReportMaker {
    private static final String[] REPORT_HEADERS = {
            "Date",
            "OperationType",
            "SourceAmount",
            "Category",
            "Description",
            "SourceWallet",
            "TargetWallet"
    };
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.withHeader(REPORT_HEADERS);

    public static void createReport(List<MoneyTransaction> transactions, String filename) {
        withOpenCsvToWrite(filename, CSV_FORMAT, csvPrinter -> {
            transactions.forEach(transaction -> printTransaction(transaction, csvPrinter));
        });

    }

    private static void printTransaction(MoneyTransaction transaction, CSVPrinter csvPrinter) {
        try {
            List<BigDecimal> amounts = getAmounts(transaction);
            csvPrinter.printRecord(
                    transaction.getDate(),
                    transaction.getOperationType(),
                    amounts.get(0),
                    transaction.getCategory(),
                    transaction.getDescription(),
                    transaction.getSourceWallet(),
                    transaction.getTargetWallet()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<BigDecimal> getAmounts(MoneyTransaction transaction) {
        List<BigDecimal> result = new ArrayList<>(2);
        Amounts amounts = transaction.getAmounts();
        BigDecimal sourceAmount = amounts.getSourceAmount().getAmount();
        BigDecimal targetAmount = Optional.ofNullable(amounts.getTargetAmount()).map(AmountWithCcy::getAmount).orElse(null);
        if (!isZeroOrNull(sourceAmount) && !isZeroOrNull(targetAmount)) {
            result.add(sourceAmount);
            result.add(targetAmount);
        } else if (!isZeroOrNull(sourceAmount)) {
            result.add(sourceAmount);
            result.add(null);
        } else if (!isZeroOrNull(targetAmount)) {
            result.add(targetAmount);
            result.add(null);
        } else {
            log.error("Transaction with no amount in report. Transaction: {}", transaction);
            result.add(null);
            result.add(null);
        }
        return result;
    }

    private static void withOpenCsvToWrite(String filename, CSVFormat format, Consumer<CSVPrinter> consumer) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename))) {
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
                consumer.accept(csvPrinter);
                csvPrinter.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isZeroOrNull(BigDecimal amount) {
        return amount == null || amount.signum() == 0;
    }

}
