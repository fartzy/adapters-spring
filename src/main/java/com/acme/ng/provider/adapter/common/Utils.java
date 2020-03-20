package com.acme.ng.provider.adapter.common;

import com.acme.ng.provider.definition.TableColumnType;
import com.acme.ng.provider.definition.TableRelation;
import com.acme.ng.provider.model.GlobalConstants;
import com.acme.ng.provider.model.ModelInstance;
import com.acme.ng.provider.model.common.ModelDefinition;
import com.acme.ng.provider.util.ModelDefUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;

import static java.util.stream.Collectors.*;

public class Utils {

    protected static final TableColumnType EMPTY_TABLE_COLUMN_TYPE = new TableColumnType();

    private Utils() {
    }

    /**
     * Returns Map<String, Set<TableColumnType>> where Key is "relatedTable and Value is Set of fields with this "relatedTable".
     *
     * @param modelDefinition
     */
    public static Map<String, Set<TableColumnType>> getForeignKeysGatheredByRelatedTable(ModelDefinition modelDefinition) {
        Set<TableColumnType> foreignKeyTableColumns = getForeignKeyColumns(modelDefinition);

        Map<String, Set<TableColumnType>> gatheredForeignKeys = new HashMap<>();

        for (TableColumnType tableColumnType : foreignKeyTableColumns) {
            String relatedTable = getRelatedTable(tableColumnType);
            gatheredForeignKeys.computeIfAbsent(relatedTable, set -> new HashSet<>());

            Set<TableColumnType> tableColumnTypes = gatheredForeignKeys.get(relatedTable);
            tableColumnTypes.add(tableColumnType);

            gatheredForeignKeys.put(relatedTable, tableColumnTypes);
        }

        return gatheredForeignKeys;
    }

    private static String getRelatedTable(TableColumnType tableColumnType) {
        Collection<TableRelation> tableRelations = ModelDefUtils.effectiveTableRelations(tableColumnType);
        return tableRelations.isEmpty() ? null : tableRelations.iterator().next().getRelatedTable();
    }

    private static String getRelatedColumn(TableColumnType tableColumnType) {
        Collection<TableRelation> tableRelations = ModelDefUtils.effectiveTableRelations(tableColumnType);
        return tableRelations.isEmpty() ? null : tableRelations.iterator().next().getRelatedColumn();
    }

    public static TableColumnType findForeignKeyGuidColumn(Set<TableColumnType> columns) {
        return columns.stream()
                .filter(tableColumnType -> GlobalConstants.GUID_KEY.equals(getRelatedColumn(tableColumnType)))
                .findFirst().orElse(EMPTY_TABLE_COLUMN_TYPE);
    }

    public static TableColumnType findForeignKeyNonGuidColumn(Set<TableColumnType> columns) {
        return columns.stream()
                .filter(tableColumnType -> !GlobalConstants.GUID_KEY.equals(getRelatedColumn(tableColumnType)))
                .findFirst().orElse(EMPTY_TABLE_COLUMN_TYPE);
    }

    public static String getGuidForeignKeyName(Set<TableColumnType> columns) {
        return columns.stream()
                .filter(tableColumnType -> GlobalConstants.GUID_KEY.equals(getRelatedColumn(tableColumnType)))
                .findFirst().orElse(EMPTY_TABLE_COLUMN_TYPE)
                .getName();
    }

    public static String getGuidForeignKeyName(Set<TableColumnType> columns, TableColumnType tableColumnType) {
        final String[] guidForeignKeyName = new String[1];

        columns.stream()
                .filter(TableColumnType::isForeignKey) //foreign keys only
                .flatMap(ct -> ct.getTableRelations().stream())
                .filter(rel -> {
                    Optional<TableRelation> first = tableColumnType.getTableRelations()
                            .stream()
                            .filter(rel2 -> rel.getRelatedTable().equals(rel2.getRelatedTable()))
                            //                            .filter(rel2 -> GlobalConstants.GUID_KEY.equals(rel.getRelatedColumn()))
                            .findFirst();
                    return first.isPresent();
                })
                .findFirst()
                .ifPresent(rel -> guidForeignKeyName[0] = rel.getRelatedColumn());

        return guidForeignKeyName[0];
    }

    public static Set<String> getNonGuidForeignKeyNames(Set<TableColumnType> columns) {
        return columns.stream()
                .filter(tableColumnType -> !GlobalConstants.GUID_KEY.equals(getRelatedColumn(tableColumnType)))
                .map(TableColumnType::getName)
                .collect(toSet());
        //TODO: return all foreign keys
    }

    public static Set<String> getPrimaryKeyColumnNames(ModelDefinition modelDefinition) {
        return modelDefinition.getTableColumns().stream()
                .filter(TableColumnType::isPrimaryKey)
                .map(TableColumnType::getName)
                .collect(toSet());
    }

    public static Map<String, Object> getColumnNamesAndValues(ModelInstance modelInstance, Set<String> columnNames) {
        Map<String, Object> columnValues = new HashMap<>();
        for (String columnName : columnNames) {
            columnValues.put(columnName, modelInstance.getBusinessData().get(columnName));
        }
        return columnValues;
    }

    public static Set<String> getDestinationPrimaryKeyColumnNames(ModelDefinition modelDefinition) {
        return modelDefinition.getTableColumns().stream()
                .filter(TableColumnType::isPrimaryKey)
                .filter(tableColumnType -> !tableColumnType.getName().equals(GlobalConstants.GUID_KEY)) //TODO DONE: Added filtering for GUID
                .map(TableColumnType::getName)
                .collect(toSet());
    }

    public static Set<String> getPrimaryAndForeignKeyColumnNames(ModelDefinition modelDefinition) {
        return modelDefinition.getTableColumns().stream()
                .filter(tableColumnType -> tableColumnType.isPrimaryKey() || tableColumnType.isForeignKey())
                .map(TableColumnType::getName)
                .collect(toSet());
    }

    public static Set<String> getPrimaryAndForeignGuidsAndHousekeepingFieldNames(ModelDefinition modelDefinition) {
        return modelDefinition.getTableColumns().stream()
                .filter(TableColumnType::isAddedFromBaseModelDefinition)
                .filter(column -> (column.isPrimaryKey() && Objects.equals(column.getName(), GlobalConstants.GUID_KEY)))
                .filter(column -> (column.isForeignKey() && Objects.equals(getRelatedColumn(column), GlobalConstants.GUID_KEY)))
                .map(TableColumnType::getName)
                .collect(toSet());
    }

    public static List<TableColumnType> getPrimaryDestinationField(ModelDefinition modelDefinition) {
        return modelDefinition.getTableColumns().stream()
                .filter(column -> (column.isPrimaryKey() && !Objects.equals(column.getName(), GlobalConstants.GUID_KEY)))
                .collect(toList());
    }

    public static String joinCompositePrimaryKey(Map<String, Object> values, List<TableColumnType> primaryKeyTableColumns) {
        return primaryKeyTableColumns.stream()
                .map(tableColumnType -> Objects.toString(tableColumnType.getName() + ":" + values.get(tableColumnType.getName())))
                .collect(joining("_"));
    }

    private static Set<TableColumnType> getForeignKeyColumns(ModelDefinition modelDefinition) {
        return modelDefinition.getTableColumns().stream()
                .filter(TableColumnType::isForeignKey)
                .collect(toSet());
    }

    public static Map<String, Object> copyBusinessDataWithLowerCaseKeys(Map<String, Object> businessData) {
        Map<String, Object> lowerCaseKeyBusinessData = new HashMap<>();

        businessData.entrySet().stream()
                .forEach(entrySet -> lowerCaseKeyBusinessData.put(entrySet.getKey().toLowerCase(), entrySet.getValue()));

        return lowerCaseKeyBusinessData;
    }

    public static Map<String, Object> copyBusinessData(Map<String, Object> businessData) {
        Map<String, Object> keyBusinessData = ObjectUtils.cloneIfPossible(businessData);

        return keyBusinessData;
    }
}
