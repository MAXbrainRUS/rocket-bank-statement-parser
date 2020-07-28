package ru.maxbrainrus.parser;

import lombok.extern.slf4j.Slf4j;
import ru.maxbrainrus.config.ConfigValue;
import ru.maxbrainrus.parser.statement.RaiffeisenStatementParser;
import ru.maxbrainrus.report.CsvReportMaker;
import ru.maxbrainrus.transaction.MoneyTransaction;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ReportGeneratorFacade {

    public static void makeReport(String inputDataFileName,
                                  String reportFileName,
                                  Map<String, ConfigValue> keyWordsToCategoryMap,
                                  @Nullable LocalDate cutDate,
                                  String sourceWallet) {
        StatementParser statementParser = new RaiffeisenStatementParser();
        List<MoneyTransaction> transactions = statementParser.parseBankStatement(inputDataFileName, sourceWallet);
        transactions = fillCategoriesAndWallets(keyWordsToCategoryMap, transactions);
        logTransactions(transactions);
        if (cutDate != null) {
            transactions = removeOldTransactions(transactions, cutDate);
        }
        CsvReportMaker.createReport(transactions, reportFileName);
    }

    private static List<MoneyTransaction> removeOldTransactions(List<MoneyTransaction> transactions, @Nullable LocalDate cutDate) {
        transactions = transactions.stream()
                .filter(moneyTransaction -> moneyTransaction.getDate().isAfter(cutDate))
                .collect(Collectors.toList());
        log.info("Transactions after cutting using cut date {}: {}", cutDate, transactions);
        return transactions;
    }

    private static void logTransactions(List<MoneyTransaction> transactions) {
        transactions.forEach(moneyTransaction -> log.info("Parsed transaction" + ": {}", moneyTransaction));
    }

    private static List<MoneyTransaction> fillCategoriesAndWallets(Map<String, ConfigValue> keyWordsToCategoryMap, List<MoneyTransaction> transactions) {
        ConfigFiller categoryWalletFiller = new ConfigFiller(keyWordsToCategoryMap);
        return categoryWalletFiller.fill(transactions);
    }

}
