package ru.maxbrainrus.migrate;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.maxbrainrus.report.CsvReportMaker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public class MigrateTool {
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy (HH:mm)");
    public static final Pattern WALLET_PATTERN = Pattern.compile("\\[([^\\[\\]]+)\\]");
    private static final Set<OperationType> OPERATION_TYPES_WITH_CATEGORY = EnumSet.of(OperationType.INCOME, OperationType.EXPENDITURE);
    public static final String RESULTS_DIRECTORY = "results";

    public static void main(String[] args) {
        withOpenExcelSheet("AllOperations.xls", (sheet) -> {
            MigrateSheetHeaderInfo sheetHeaderInfo = getSheetHeaderInfo(sheet);
            List<MoneyTransaction> transactions = StreamSupport.stream(sheet.spliterator(), false)
                    .filter(row -> row.getRowNum() > sheetHeaderInfo.getHeaderRowIndex())
                    .map(row -> readMoneyTransaction(row, sheetHeaderInfo))
                    .filter(transaction -> !isDeletedTransaction(transaction))
                    .collect(Collectors.toList());

            System.out.println("Found wallets:");
            transactions.stream()
                    .flatMap(transaction -> Stream.of(
                            transaction.getSourceWallet(),
                            transaction.getTargetWallet()))
                    .distinct()
                    .forEach(System.out::println);

            new File(RESULTS_DIRECTORY).mkdirs();

            CsvReportMaker.createReport(transactions, walletNameToReportFilename("convertedOperations"));
        });

    }

    private static boolean isDeletedTransaction(MoneyTransaction transaction) {
        Amounts amounts = transaction.getAmounts();
        if (isZeroOrNull(amounts.getSourceAmount().getAmount()) &&
                isZeroOrNull(amounts.getTargetAmount().getAmount())) {
            log.info("Found transaction with zero amount (deleted) {}", transaction);
            return true;
        }
        return false;
    }

    public static boolean isZeroOrNull(BigDecimal amount) {
        return amount == null || amount.signum() == 0;
    }

    private static String walletNameToReportFilename(String walletName) {
        return new File(RESULTS_DIRECTORY, walletName + ".csv").getPath();
    }

    private static MoneyTransaction readMoneyTransaction(Row row, MigrateSheetHeaderInfo sheetHeaderInfo) {
        String description = extractDescription(row, sheetHeaderInfo.getDescription().getColNum());
        List<String> wallets = extractWallets(description);
        OperationType operationType = extractOperationType(row, sheetHeaderInfo.getOperationType().getColNum());
        Amounts amounts = getAmounts(row, sheetHeaderInfo, operationType);

        return MoneyTransaction.builder()
                .operationType(operationType)
                .date(extractDate(row, sheetHeaderInfo.getDateTime().getColNum()))
                .amounts(amounts)
                .sourceWallet(wallets.get(0))
                .targetWallet(operationType == OperationType.TRANSFER ? wallets.get(1) : null)
                .description(extractComment(row, sheetHeaderInfo.getComment().getColNum()))
                .category(getCategory(description, operationType))
                .build();
    }

    private static Amounts getAmounts(Row row, MigrateSheetHeaderInfo sheetHeaderInfo, OperationType operationType) {
        AmountWithCcy income = AmountWithCcy.builder()
                .amount(extractAmount(row, sheetHeaderInfo.getSumIncome().getColNum()))
                .ccy(extractCcy(row, sheetHeaderInfo.getCcyIncome().getColNum()))
                .build();
        AmountWithCcy outcome = AmountWithCcy.builder()
                .amount(extractAmount(row, sheetHeaderInfo.getSumOutcome().getColNum()))
                .ccy(extractCcy(row, sheetHeaderInfo.getCcyOutcome().getColNum()))
                .build();
        Amounts.AmountsBuilder builder = Amounts.builder();

        if (operationType == OperationType.TRANSFER) {
            builder
                    .sourceAmount(outcome)
                    .targetAmount(income);
        } else {
            builder
                    .sourceAmount(income)
                    .targetAmount(outcome);
        }
        return builder.build();

    }

    private static String extractCcy(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (isStringCell(cell)) {
            String commentValue = cell.getStringCellValue();
            log.debug("Processing ccy from cell value: {}", commentValue);
            return commentValue;
        }
        return null;
    }

    private static String getCategory(String description, OperationType operationType) {
        return OPERATION_TYPES_WITH_CATEGORY.contains(operationType) ?
                extractCategory(description) : null;
    }

    private static <T> List<T> union(List<T> list1, List<T> list2) {
        List<T> newList = new ArrayList<>(list1.size() + list2.size());
        newList.addAll(list1);
        newList.addAll(list2);
        return newList;
    }

    private static String extractCategory(String description) {
        int startIndex = description.indexOf(":");
        if (startIndex == -1) {
            return null;
        }
        String substring = description.substring(startIndex + 1);
        int endIndex = substring.indexOf("(");
        if (endIndex != -1) {
            substring = substring.substring(0, endIndex);
        }
        return substring.trim();
    }

    private static String extractComment(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (isStringCell(cell)) {
            String commentValue = cell.getStringCellValue();
            log.debug("Processing comment from cell value: {}", commentValue);
            return commentValue;
        }
        return null;
    }

    private static String extractDescription(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (isStringCell(cell)) {
            String descriptionValue = cell.getStringCellValue();
            log.debug("Processing description from cell value: {}", descriptionValue);
            return descriptionValue;
        }
        return null;
    }

    private static List<String> extractWallets(String description) {
        Matcher matcher = WALLET_PATTERN.matcher(description);
        List<String> wallets = new ArrayList<>();
        while (matcher.find()) {
            String wallet = matcher.group(1);
            wallets.add(wallet);
        }
        if (description.startsWith("Мы дали в долг") ||
                description.startsWith("Нам вернули долг")) {
            wallets = Arrays.asList(wallets.get(1), "Долги");
        }
        log.info("Found wallets: {}", wallets);
        return wallets;
    }

    private static BigDecimal extractAmount(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (isNumericCell(cell)) {
            double amountValue = cell.getNumericCellValue();
            log.debug("Processing amount cell value {}", amountValue);
            return new BigDecimal(amountValue).setScale(2, RoundingMode.HALF_EVEN);
        }
        return null;
    }

    private static LocalDate extractDate(Row row, int dateColumnIndex) {
        Cell cell = row.getCell(dateColumnIndex);
        if (isStringCell(cell)) {
            String dateString = cell.getStringCellValue();
            log.debug("Processing date cell value: {}", dateString);
            return parseDate(dateString, DATE_FORMATTER);
        }
        return null;
    }

    private static OperationType extractOperationType(Row row, int operatoinTypeColumnIndex) {
        Cell cell = row.getCell(operatoinTypeColumnIndex);
        if (isStringCell(cell)) {
            String operationTypeString = cell.getStringCellValue();
            log.debug("Processing operation type cell value: {}", operationTypeString);
            return parseOperationType(operationTypeString);
        }
        return null;
    }

    private static OperationType parseOperationType(String cellValue) {
        switch (cellValue) {
            case "Перемещение":
            case "Обмен валюты":
            case "Мы дали в долг":
            case "Нам вернули долг":
                return OperationType.TRANSFER;
            case "Расход":
                return OperationType.EXPENDITURE;
            case "Доход":
                return OperationType.INCOME;
            case "Ввод / изменение остатка":
                return OperationType.ADJUSTMENT;
            default:
                throw new RuntimeException("Unexpected operation type");

        }
    }

    private static boolean isStringCell(Cell cell) {
        return cell != null && cell.getCellTypeEnum() == CellType.STRING;
    }

    private static boolean isNumericCell(Cell cell) {
        return cell != null && cell.getCellTypeEnum() == CellType.NUMERIC;
    }

    private static LocalDate parseDate(String dateString, DateTimeFormatter formatter) {
        try {
            LocalDate date = LocalDate.parse(dateString, formatter);
            log.debug("Date {} is recognized as {}", dateString, date);
            return date;
        } catch (DateTimeParseException e) {
            log.debug("Date {} can not be parsed as date", dateString);
            return null;
        }
    }

    private static MigrateSheetHeaderInfo getSheetHeaderInfo(Sheet sheet) {
        return MigrateSheetHeaderInfo.builder()
                .dateTime(getColumnInfoWithStringValue(sheet, "Дата, время"))
                .operationType(getColumnInfoWithStringValue(sheet, "Тип операции"))
                .sumIncome(getColumnInfoWithStringValue(sheet, "СуммаПоступления"))
                .ccyIncome(getColumnInfoWithStringValue(sheet, "ВалютаПоступления"))
                .sumOutcome(getColumnInfoWithStringValue(sheet, "СуммаСписания"))
                .ccyOutcome(getColumnInfoWithStringValue(sheet, "ВалютаСписания"))
                .description(getColumnInfoWithStringValue(sheet, "Описание операции"))
                .comment(getColumnInfoWithStringValue(sheet, "Комментарий"))
                .build();
    }

    private static ColumnInfo getColumnInfoWithStringValue(Sheet sheet, String cellValue) {
        return new ColumnInfo(getFirstCellWithStringValue(sheet, cellValue));
    }

    private static CellAddress getFirstCellWithStringValue(Sheet sheet, String cellValue) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell != null && cell.getCellTypeEnum() == CellType.STRING && cellValue.equals(cell.getStringCellValue().trim())) {
                    CellAddress cellAddress = cell.getAddress();
                    log.info("Found cell with '{}' value on position {}", cellValue, cellAddress);
                    return cellAddress;
                }
            }
        }
        throw new IllegalStateException(String.format("Cell with '%s' not found", cellValue));
    }

    public static void withOpenExcelSheet(String pathname, Consumer<Sheet> consumer) {
        try (FileInputStream file = new FileInputStream(new File(pathname))) {
            try (Workbook workbook = getWorkbook(file, pathname)) {
                Sheet sheet = workbook.getSheetAt(0);
                consumer.accept(sheet);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Workbook getWorkbook(FileInputStream file, String pathname) throws IOException {
        if (pathname.endsWith(".xls")) {
            return new HSSFWorkbook(file);
        } else if (pathname.endsWith(".xlsx")) {
            return new XSSFWorkbook(file);
        } else {
            throw new IllegalArgumentException("File extension is not supported. Only 'xls' and 'xlsx' are supported");
        }
    }

}
