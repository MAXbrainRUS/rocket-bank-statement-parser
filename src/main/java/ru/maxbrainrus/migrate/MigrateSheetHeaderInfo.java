package ru.maxbrainrus.migrate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
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
}
