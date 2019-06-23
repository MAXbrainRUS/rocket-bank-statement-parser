package ru.maxbrainrus.parser;

import ru.maxbrainrus.report.CsvReportMaker;
import ru.maxbrainrus.transaction.MoneyTransaction;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RocketParserController {

    public static void makeReport(String inputDataFileName, String reportFileName, Map<String, String> keyWordsToCategoryMap, @Nullable LocalDate cutDate) {
        KeyWordCategoryFiller categoryFiller = new KeyWordCategoryFiller(keyWordsToCategoryMap);
        RocketPdfParser rocketPdfParser = new RocketPdfParser(categoryFiller);
        List<MoneyTransaction> transactions = rocketPdfParser.parsePdf(inputDataFileName);

        if (cutDate != null) {
            transactions = transactions.stream()
                    .filter(moneyTransaction -> moneyTransaction.getDate().isAfter(cutDate))
                    .collect(Collectors.toList());
        }

        CsvReportMaker.createReport(transactions, reportFileName);
    }
}
