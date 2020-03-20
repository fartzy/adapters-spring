package com.acme.ng.provider.adapter.common.service.sql.impl.internal;

import com.acme.ng.provider.definition.TableColumnType;
import com.acme.ng.provider.model.common.ModelDefinition;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created on 4/30/2018.
 */
public class SQLTableMetadata {

    private final DbTable table;
    private final DbSchema schema;
    private final ModelDefinition modelDefinition;
    private final List<SQLColumnMetadata> columns;

    public SQLTableMetadata(DbTable table, DbSchema schema, ModelDefinition modelDefinition, List<SQLColumnMetadata> columns) {
        this.table = table;
        this.schema = schema;
        this.modelDefinition = modelDefinition;
        this.columns = columns;
    }

    public DbTable getTable() {
        return table;
    }

    public DbSchema getSchema() {
        return schema;
    }

    public ModelDefinition getModelDefinition() {
        return modelDefinition;
    }

    public List<SQLColumnMetadata> getColumns() {
        return columns;
    }

    public List<DbColumn> getColumnsByPredicate(Predicate<TableColumnType> predicate) {
        return columns.stream()
                .filter(columnMetadata -> predicate.test(columnMetadata.getColumnType()))
                .map(SQLColumnMetadata::getDbColumn)
                .collect(Collectors.toList());
    }

}
