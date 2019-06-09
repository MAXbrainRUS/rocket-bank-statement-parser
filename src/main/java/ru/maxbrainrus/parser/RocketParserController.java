package ru.maxbrainrus.parser;

import ru.maxbrainrus.report.CsvReportMaker;
import ru.maxbrainrus.transaction.MoneyTransaction;

import java.util.List;
import java.util.Map;

public class RocketParserController {

    public static void makeReport(String inputDataFileName, String reportFileName, Map<String, String> keyWordsToCategoryMap) {
        KeyWordCategoryWalletFiller categoryWalletFiller = new KeyWordCategoryWalletFiller(keyWordsToCategoryMap);
        RocketPdfParser rocketPdfParser = new RocketPdfParser(categoryWalletFiller);
        List<MoneyTransaction> transactions = rocketPdfParser.parsePdf(inputDataFileName);
        CsvReportMaker.createReport(transactions, reportFileName);
    }
}
