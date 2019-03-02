package ru.maxbrainrus.parser;

import ru.maxbrainrus.report.XlsReportMaker;

import java.util.List;
import java.util.Map;

public class RocketParserController {

    public static void makeReport(String inputDataFileName, String reportFileName, Map<String, String> keyWordsToCategoryMap) {
        KeyWordCategoryFiller categoryFiller = new KeyWordCategoryFiller(keyWordsToCategoryMap);
        RocketPdfParser rocketPdfParser = new RocketPdfParser(categoryFiller);
        List<Transaction> transactions = rocketPdfParser.parsePdf(inputDataFileName);
        XlsReportMaker.createReport(transactions, reportFileName);
    }
}
