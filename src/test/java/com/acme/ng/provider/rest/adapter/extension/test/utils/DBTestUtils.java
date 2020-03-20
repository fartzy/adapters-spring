package com.acme.ng.provider.rest.adapter.extension.test.utils;

import com.acme.ng.provider.definition.TableColumnType;
import com.acme.ng.provider.model.enums.ColumnDataType;

import java.util.Scanner;

/**
 * 
 */
public class DBTestUtils {
    public static String readScript(String script, Class<?> target) {
        Scanner scanner = new Scanner(target.getResourceAsStream(script));
        scanner.useDelimiter("\\A");
        return scanner.next();
    }

    public static TableColumnType createTableColumn(String columnName, ColumnDataType columnDataType, boolean isPrimaryKey, boolean isBusinessKey, String sequenceName, boolean skipCompare) {
        TableColumnType tableColumnType = new TableColumnType();
        tableColumnType.setName(columnName);
        tableColumnType.setType(columnDataType);
        tableColumnType.setPrimaryKey(isPrimaryKey);
        tableColumnType.setBusinessKey(isBusinessKey);
        tableColumnType.setSequenceName(sequenceName);
        tableColumnType.setSkipCompare(skipCompare);
        return tableColumnType;
    }

    public static TableColumnType createNumericTableColumnWithPrecisionAndScale(String columnName, int precision, int scale, boolean isPrimaryKey, boolean isBusinessKey, String sequenceName, boolean skipCompare) {
        TableColumnType tableColumnType = createTableColumn(columnName, ColumnDataType.NUMERIC, isPrimaryKey, isBusinessKey, sequenceName, skipCompare);
        tableColumnType.setPrecision(precision);
        tableColumnType.setScale(scale);
        return tableColumnType;
    }
}
