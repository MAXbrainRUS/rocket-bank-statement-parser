package ru.maxbrainrus.report;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import ru.maxbrainrus.migrate.MoneyTransaction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

public class CsvReportMaker {
    public static final String[] REPORT_HEADERS = {
            "Date",
            "OperationType",
            "AmountArrival",
            "AmountExpenditure",
            "Category",
            "Description",
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
                    transaction.getAmountArrival(),
                    transaction.getAmountExpenditure(),
                    transaction.getCategory(),
                    transaction.getDescription(),
                    transaction.getTargetWallet()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
