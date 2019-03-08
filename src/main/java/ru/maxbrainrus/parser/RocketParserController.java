package ru.maxbrainrus.parser;

import ru.maxbrainrus.migrate.MoneyTransaction;
import ru.maxbrainrus.report.CsvReportMaker;

import java.util.List;
import java.util.Map;

public class RocketParserController {

    public static void makeReport(String inputDataFileName, String reportFileName, Map<String, String> keyWordsToCategoryMap) {
        KeyWordCategoryFiller categoryFiller = new KeyWordCategoryFiller(keyWordsToCategoryMap);
        RocketPdfParser rocketPdfParser = new RocketPdfParser(categoryFiller);
        List<MoneyTransaction> transactions = rocketPdfParser.parsePdf(inputDataFileName);
        CsvReportMaker.createReport(transactions, reportFileName);
    }
}
