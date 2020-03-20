package com.acme.ng.provider.adapter.common.service.sql.impl.internal;

import com.acme.ng.provider.adapter.common.service.columns.TableColumnService;
import com.acme.ng.provider.definition.TableColumnType;
import com.acme.ng.provider.model.common.ModelDefinition;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.acme.ng.provider.adapter.common.service.columns.TableColumnService.ALL_COLUMNS_PREDICATE;

/**
 * Created 4/30/2018.
 */
@Component
public class TableMetadataFactory {

    @Autowired
    private TableColumnService tableColumnService;

    public SQLTableMetadata createTableMetadata(ModelDefinition modelDefinition, String schemaName) {

        Assert.hasText(modelDefinition.getTableName(), "Table name is required");

        DbSchema schema = StringUtils.isEmpty(schemaName) ?
                new DbSpec().addDefaultSchema() : new DbSpec().addSchema(schemaName);

        DbTable table = schema.addTable(modelDefinition.getTableName());
        List<SQLColumnMetadata> columns = new ArrayList<>();
        for (TableColumnType columnType : tableColumnService.sortAndExtractNonHousekeepingColumns(modelDefinition, ALL_COLUMNS_PREDICATE)) {
            Assert.hasText(columnType.getName(), "Column name is required");
            Assert.hasText(columnType.getName(), "Column type is required");

            DbColumn dbColumn = table.addColumn(columnType.getName(), columnType.getType().name(),
                    columnType.getSize(), columnType.getScale());

            columns.add(new SQLColumnMetadata(columnType, dbColumn));
        }

        return new SQLTableMetadata(table, schema, modelDefinition, columns);
    }
}
