package com.acme.ng.provider.adapter.common.service.sql.impl.ansi;

import com.acme.ng.provider.adapter.common.model.DatabaseStatementType;
import com.acme.ng.provider.adapter.common.service.columns.TableColumnService;
import com.acme.ng.provider.adapter.common.service.sql.SQLStatementGeneratorService;
import com.acme.ng.provider.adapter.common.service.sql.impl.internal.SQLColumnMetadata;
import com.acme.ng.provider.adapter.common.service.sql.impl.internal.SQLTableMetadata;
import com.acme.ng.provider.adapter.common.service.sql.impl.internal.TableMetadataFactory;
import com.acme.ng.provider.definition.TableColumnType;
import com.acme.ng.provider.model.common.ModelDefinition;
import com.healthmarketscience.sqlbuilder.*;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.acme.ng.provider.adapter.common.service.columns.TableColumnService.TABLE_COLUMN_THAT_DONT_HAVE_SEQUENCE_NAME_PLUS_GUID;
import static com.acme.ng.provider.adapter.common.service.columns.TableColumnService.TABLE_COLUMN_THAT_HAVE_SEQUENCE_NAME;
import static com.acme.ng.provider.adapter.common.service.columns.impl.TableColumnServiceImpl.*;
import static java.util.stream.Collectors.toList;

/**
 * @author Alexander Lukyanchikov
 * @date 4/8/18
 */
@Service
@Qualifier("ansi")
public class AnsiSQLStatementGeneratorServiceImpl implements SQLStatementGeneratorService {

    @Autowired
    protected TableColumnService tableColumnService;

    @Autowired
    protected TableMetadataFactory tableMetadataFactory;

    @Value("${legacy.table.prefix}")
    private String legacyTablePrefix;

    @Override
    public Map<DatabaseStatementType, String> generateSQLStatements(ModelDefinition modelDefinition, String schemaName) {

        SQLTableMetadata metadata = tableMetadataFactory.createTableMetadata(modelDefinition, schemaName);

        EnumMap<DatabaseStatementType, String> statements = new EnumMap<>(DatabaseStatementType.class);
        statements.put(DatabaseStatementType.INSERT, generateInsert(metadata));
        statements.put(DatabaseStatementType.UPDATE_BY_PK, generateUpdateByPrimaryKeys(metadata));
        statements.put(DatabaseStatementType.SELECT_BY_PK, generateSelectByPrimaryKeys(metadata));
        statements.put(DatabaseStatementType.SELECT_BY_BK, generateSelectByBusinessKeys(metadata));

        // Tactical fix to avoid table name collisions between CPF and PTDM.  When there is a collision
        // CPF model and table names will be prefixed by LEGACY_ (e.g., LEGACY_PROV).   The prefix must be removed
        // from the generated SQL statements since there is no LEGACY_PROV for instance table in CPF.
        for (Map.Entry<DatabaseStatementType, String> entry : statements.entrySet()) {
            entry.setValue(StringUtils.replace(entry.getValue(), legacyTablePrefix, StringUtils.EMPTY));
        }

        return statements;
    }

    protected String generateInsert(SQLTableMetadata sqlTableMetadata) {

        List<DbColumn> columnsThatAreGeneratedBySequence = sqlTableMetadata.getColumnsByPredicate(TABLE_COLUMN_THAT_HAVE_SEQUENCE_NAME);
        List<DbColumn> otherColumns = sqlTableMetadata.getColumnsByPredicate(TABLE_COLUMN_THAT_DONT_HAVE_SEQUENCE_NAME_PLUS_GUID);

        columnsThatAreGeneratedBySequence.sort(Comparator.comparing(DbColumn::getColumnNameSQL));

        List<SQLColumnMetadata> columnMetadataList = sqlTableMetadata.getColumns().stream()
                .filter(columnMetadata -> columnMetadata.getColumnType().getSequenceName() != null
                        && columnMetadata.getColumnType().getSequenceName().length() > 0)
                .sorted(Comparator.comparing(columnMetadata -> columnMetadata.getColumnType().getName()))
                .collect(toList());

        return new InsertQuery(sqlTableMetadata.getTable())
                .addPreparedColumnCollection(otherColumns)
                .addCustomColumns(
                        columnsThatAreGeneratedBySequence.stream()
                                .map(DbColumn::getColumnNameSQL)
                                .toArray(),
                        columnMetadataList.stream()
                                .map(column -> getSequenceGeneratedValue(getSequenceNameWithSchema(sqlTableMetadata.getSchema(), column.getColumnType())))
                                .toArray()
                )
                .validate()
                .toString();
    }

    protected String generateUpdateByPrimaryKeys(SQLTableMetadata sqlTableMetadata) {

        QueryPreparer preparer = new QueryPreparer();
        UpdateQuery query = new UpdateQuery(sqlTableMetadata.getTable());

        sqlTableMetadata.getColumnsByPredicate(NON_PK_COLUMNS_PREDICATE)
                .forEach(nonPKColumn -> query.addSetClause(nonPKColumn, preparer.getNewPlaceHolder()));

        sqlTableMetadata.getColumnsByPredicate(PK_COLUMNS_PREDICATE)
                .forEach(pKColumn -> query.addCondition(BinaryCondition.equalTo(pKColumn, preparer.getNewPlaceHolder())));

        return query.validate().toString();
    }

    protected String generateSelectByPrimaryKeys(SQLTableMetadata sqlTableMetadata) {

        QueryPreparer preparer = new QueryPreparer();
        SelectQuery query = new SelectQuery().addAllColumns();

        sqlTableMetadata.getColumnsByPredicate(PK_COLUMNS_PREDICATE)
                .forEach(pKColumn -> query.addCondition(BinaryCondition.equalTo(pKColumn, preparer.getNewPlaceHolder())));

        return query.validate().toString();
    }

    protected String generateSelectByBusinessKeys(SQLTableMetadata sqlTableMetadata) {

        QueryPreparer preparer = new QueryPreparer();
        SelectQuery query = new SelectQuery().addAllColumns();

        if (tableColumnService.definitionContainsBKColumns(sqlTableMetadata.getModelDefinition())) {
            // regular table case (has BK columns)
            List<DbColumn> bKColumns = sqlTableMetadata.getColumnsByPredicate(BK_COLUMNS_PREDICATE);
            bKColumns.forEach(pKColumn -> query.addCondition(BinaryCondition.equalTo(pKColumn, preparer.getNewPlaceHolder())));
        } else {
            // bridge table case (no BK columns)
            List<DbColumn> nonGuidFKColumns = sqlTableMetadata.getColumnsByPredicate(PK_COLUMNS_PREDICATE);
            nonGuidFKColumns.forEach(nonGuidFKColumn -> query.addCondition(BinaryCondition.equalTo(nonGuidFKColumn, preparer.getNewPlaceHolder())));
        }

        return query.validate().toString();
    }

    protected Expression getSequenceGeneratedValue(String sequenceName) {
        return new AnsiSequenceGeneratedValue(sequenceName);
    }

    protected String getSequenceNameWithSchema(DbSchema schema, TableColumnType column) {
        return StringUtils.isBlank(schema.getName()) ?
                column.getSequenceName() : String.format("%s.%s", schema.getName(), column.getSequenceName());
    }
}
