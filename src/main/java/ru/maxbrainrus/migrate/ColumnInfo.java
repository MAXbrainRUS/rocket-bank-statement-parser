package ru.maxbrainrus.migrate;

import org.apache.poi.ss.util.CellAddress;

public class ColumnInfo {

    private final CellAddress cellAddress;

    public ColumnInfo(CellAddress cellAddress) {
        this.cellAddress = cellAddress;
    }


    public int getColNum() {
        return cellAddress.getColumn();
    }

    public int getHeaderRow() {
        return cellAddress.getRow();
    }
}
