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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static ru.maxbrainrus.migrate.MigrateTool.isZeroOrNull;

@Slf4j
public class CsvReportMaker {
    public static final String[] REPORT_HEADERS = {
            "Date",
            "OperationType",
            "Amount",
            "SourceCurrency",
            "DestinationAmount",
            "DestinationCurrency",
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
            List<BigDecimal> amounts = getAmounts(transaction);
            BigDecimal destinationAmount = amounts.get(1);
            csvPrinter.printRecord(
                    transaction.getDate(),
                    transaction.getOperationType(),
                    amounts.get(0),
                    transaction.getAmounts().getArrival().getCcy(),
                    destinationAmount,
                    destinationAmount == null ? null : transaction.getAmounts().getExpenditure().getCcy(),
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
        BigDecimal income = transaction.getAmounts().getArrival().getAmount();
        BigDecimal outcome = transaction.getAmounts().getExpenditure().getAmount();
        if (!isZeroOrNull(income) && !isZeroOrNull(outcome)) {
            result.add(income);
            result.add(outcome);
        } else {
            result.add(getNonZeroAmount(transaction));
            result.add(null);
        }
        return result;
    }

    private static BigDecimal getNonZeroAmount(MoneyTransaction transaction) {
        BigDecimal amountArrival = transaction.getAmounts().getArrival().getAmount();
        BigDecimal amountExpenditure = transaction.getAmounts().getExpenditure().getAmount();
        if (!isZeroOrNull(amountArrival)) {
            return amountArrival;
        } else {
            if (!isZeroOrNull(amountExpenditure)) {
                return amountExpenditure;
            } else {
                log.error("Transaction with no amount in report. Transaction: {}", transaction);
                return null;
            }
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
