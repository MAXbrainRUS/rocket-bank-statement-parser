package ru.maxbrainrus.parser;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ReportMaker {
    private static final String fmt = "dd/MM/yyyy";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(fmt);

    public static void createReport(List<Transaction> transactions, String filename) {
        try (HSSFWorkbook workbook = new HSSFWorkbook()) {
            HSSFSheet sheet = workbook.createSheet("FirstSheet");

            HSSFRow rowhead = sheet.createRow((short) 0);
            rowhead.createCell(0).setCellValue("date");
            rowhead.createCell(1).setCellValue("transactionType");
            rowhead.createCell(2).setCellValue("purse");
            rowhead.createCell(3).setCellValue("amountArrival");
            rowhead.createCell(4).setCellValue("amountExpenditure");
            rowhead.createCell(5).setCellValue("category");

            int i = 1;

            for (Transaction dataTransaction : transactions) {
                HSSFRow row = sheet.createRow((short) i++);
                row.createCell(0).setCellValue(dataTransaction.getDate().format(formatter));
                row.createCell(1).setCellValue(dataTransaction.getDescription());
                row.createCell(2).setCellValue("Рокет");
                Optional.ofNullable(dataTransaction.getAmountArrival()).ifPresent(amount -> row.createCell(3).setCellValue(amount.toPlainString()));
                Optional.ofNullable(dataTransaction.getAmountExpenditure()).ifPresent(amount -> row.createCell(4).setCellValue(amount.toPlainString()));
                Optional.ofNullable(dataTransaction.getCategory()).ifPresent(category -> row.createCell(5).setCellValue(category));
            }

            try (FileOutputStream fileOut = new FileOutputStream(filename)) {
                workbook.write(fileOut);
            }
        } catch (Exception e) {
            System.err.println("An error occurred while generating the excel report.");
            throw new RuntimeException(e);
        }
        System.out.println("Your excel file has been generated!");
    }
}
