package com.acme.ng.provider.adapter.common.service.columns;

import com.acme.ng.provider.definition.TableColumnType;
import com.acme.ng.provider.model.GlobalConstants;
import com.acme.ng.provider.model.common.ModelDefinition;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Predicate;

/**
 * 
 * @date 4/11/18
 */
public interface TableColumnService {

    Predicate<TableColumnType> BK_COLUMNS_PREDICATE = TableColumnType::isBusinessKey;
    Predicate<TableColumnType> PK_COLUMNS_PREDICATE = column -> column.isPrimaryKey() && !column.getName().equals(GlobalConstants.GUID_KEY);
    Predicate<TableColumnType> PK_COLUMNS_PREDICATE_previos_version = TableColumnType::isPrimaryKey;
    Predicate<TableColumnType> PK_COLUMNS_NO_GUID_PREDICATE = tableColumnType -> tableColumnType.isPrimaryKey() && !tableColumnType.getName().equals(GlobalConstants.GUID_KEY);
    Predicate<TableColumnType> NON_PK_COLUMNS_PREDICATE = PK_COLUMNS_PREDICATE_previos_version.negate();
    Predicate<TableColumnType> ALL_COLUMNS_PREDICATE = column -> true;
    Predicate<TableColumnType> ALL_COLUMNS_WITHOUT_THOSE_THAT_ARE_GENERATED_BY_SEQUENCES_PREDICATE = column -> StringUtils.isEmpty(column.getSequenceName()) && !column.getName().equals(GlobalConstants.GUID_KEY);
    Predicate<TableColumnType> TABLE_COLUMN_THAT_HAVE_SEQUENCE_NAME = column -> StringUtils.hasText(column.getSequenceName());
    Predicate<TableColumnType> TABLE_COLUMN_THAT_DONT_HAVE_SEQUENCE_NAME_PLUS_GUID = column -> column.getSequenceName() == null || column.getSequenceName().length() == 0 && !column.getName().equals(GlobalConstants.GUID_KEY);

    List<TableColumnType> sortAndExtractNonHousekeepingColumns(ModelDefinition modelDefinition, Predicate<TableColumnType> predicate);

    boolean definitionContainsBKColumns(ModelDefinition modelDefinition);

}
