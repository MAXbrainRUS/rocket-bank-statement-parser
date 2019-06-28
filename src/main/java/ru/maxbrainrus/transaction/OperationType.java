package ru.maxbrainrus.transaction;

public enum OperationType {
    EXPENDITURE("Расход"),
    INCOME("Доход"),
    TRANSFER("Перевод средств"),
    ;

    private final String exportName;

    OperationType(String exportName) {

        this.exportName = exportName;
    }

    public String getExportName() {
        return exportName;
    }
}
