package com.acme.ng.provider.adapter.common.service.sql.impl.internal;

import com.acme.ng.provider.definition.TableColumnType;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;

/**
 * Created  4/30/2018.
 */
public class SQLColumnMetadata {

    private final TableColumnType columnType;
    private final DbColumn dbColumn;

    public SQLColumnMetadata(TableColumnType columnType, DbColumn dbColumn) {
        this.columnType = columnType;
        this.dbColumn = dbColumn;
    }

    public TableColumnType getColumnType() {
        return columnType;
    }

    public DbColumn getDbColumn() {
        return dbColumn;
    }
}
