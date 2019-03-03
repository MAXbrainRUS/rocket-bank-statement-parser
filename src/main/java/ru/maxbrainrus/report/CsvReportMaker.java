package ru.maxbrainrus.report;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import ru.maxbrainrus.migrate.MoneyTransaction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

import static ru.maxbrainrus.migrate.MigrateTool.isZeroOrNull;

@Slf4j
public class CsvReportMaker {
    public static final String[] REPORT_HEADERS = {
            "Date",
            "OperationType",
            "Amount",
            "Category",
            "Description",
            "SourceWallet",
            "TargetWallet"
    };
    public static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.withHeader(REPORT_HEADERS);

    public static void createReport(List<MoneyTransaction> transactions, String filename) {
        withOpenCsvToWrite(filename, CSV_FORMAT, csvPrinter -> {
            transactions.forEach(transaction -> printTransaction(transaction, csvPrinter));
        });

    }

    private static void printTransaction(MoneyTransaction transaction, CSVPrinter csvPrinter) {
        try {
            csvPrinter.printRecord(
                    transaction.getDate(),
                    transaction.getOperationType(),
                    getAmount(transaction),
                    transaction.getCategory(),
                    transaction.getDescription(),
                    transaction.getSourceWallet(),
                    transaction.getTargetWallet()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static BigDecimal getAmount(MoneyTransaction transaction) {
        if (!isZeroOrNull(transaction.getAmountArrival())) {
            return transaction.getAmountArrival();
        } else if (!isZeroOrNull(transaction.getAmountExpenditure())) {
            return transaction.getAmountExpenditure();
        } else {
            log.error("Transaction with no amount in report. Transaction: {}", transaction);
            return null;
        }
    }

    private static BigDecimal nullIfZero(BigDecimal amount) {
        return amount.signum() == 0 ? null : amount;
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
}
