package ru.maxbrainrus.parser;

import lombok.extern.slf4j.Slf4j;
import ru.maxbrainrus.report.CsvReportMaker;
import ru.maxbrainrus.transaction.MoneyTransaction;

import java.util.List;
import java.util.Map;

@Slf4j
public class RocketParserController {

    public static void makeReport(String inputDataFileName, String reportFileName, Map<String, String> keyWordsToCategoryMap) {
        List<MoneyTransaction> transactions = parsePdf(inputDataFileName);
        transactions = fillCategoriesAndWallets(keyWordsToCategoryMap, transactions);
        logTransactions(transactions);
        CsvReportMaker.createReport(transactions, reportFileName);
    }

    private static void logTransactions(List<MoneyTransaction> transactions) {
        transactions.forEach(moneyTransaction -> log.info("Parsed transaction: {}", moneyTransaction));
    }

    private static List<MoneyTransaction> fillCategoriesAndWallets(Map<String, String> keyWordsToCategoryMap, List<MoneyTransaction> transactions) {
        KeyWordCategoryWalletFiller categoryWalletFiller = new KeyWordCategoryWalletFiller(keyWordsToCategoryMap);
        transactions = categoryWalletFiller.fillCategoryOrWallet(transactions);
        return transactions;
    }

    private static List<MoneyTransaction> parsePdf(String inputDataFileName) {
        RocketPdfParser rocketPdfParser = new RocketPdfParser();
        return rocketPdfParser.parsePdf(inputDataFileName);
    }
}
