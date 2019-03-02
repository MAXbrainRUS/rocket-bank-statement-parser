package ru.maxbrainrus.migrate;

import lombok.Value;

@Value
public class MigrateSheetHeaderInfo {
    private final ColumnInfo dateTime;
    private final ColumnInfo operationType;
    private final ColumnInfo sumIncome;
    private final ColumnInfo ccyIncome;
    private final ColumnInfo sumOutcome;
    private final ColumnInfo ccyOutcome;
    private final ColumnInfo description;
    private final ColumnInfo comment;
    private final int headerRowIndex;

    public MigrateSheetHeaderInfo(ColumnInfo dateTime, ColumnInfo operationType, ColumnInfo sumIncome, ColumnInfo ccyIncome, ColumnInfo sumOutcome, ColumnInfo ccyOutcome, ColumnInfo description, ColumnInfo comment) {
        this.dateTime = dateTime;
        this.operationType = operationType;
        this.sumIncome = sumIncome;
        this.ccyIncome = ccyIncome;
        this.sumOutcome = sumOutcome;
        this.ccyOutcome = ccyOutcome;
        this.description = description;
        this.comment = comment;
        this.headerRowIndex = dateTime.getHeaderRow();
    }
}
