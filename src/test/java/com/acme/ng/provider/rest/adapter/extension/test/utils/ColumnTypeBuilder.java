package com.acme.ng.provider.rest.adapter.extension.test.utils;

import com.acme.ng.provider.definition.TableColumnType;
import com.acme.ng.provider.definition.TableRelation;
import com.acme.ng.provider.model.enums.ColumnDataType;

import java.util.Collections;
import java.util.List;

import static com.acme.ng.provider.model.GlobalConstants.GUID_KEY;

public class ColumnTypeBuilder {

    private String name;
    private ColumnDataType type = ColumnDataType.CHAR;
    private Integer size = 10;
    private boolean primaryKey;
    private boolean foreignKey;
    private boolean indexed;
    private boolean addedFromBaseModelDefinition;
    private TableRelation tableRelation = new TableRelation();
    private List<TableRelation> tableRelations;

    public ColumnTypeBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public ColumnTypeBuilder setType(ColumnDataType type) {
        this.type = type;
        return this;
    }

    public ColumnTypeBuilder setSize(Integer size) {
        this.size = size;
        return this;
    }

    public ColumnTypeBuilder setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
        return this;
    }

    public ColumnTypeBuilder setForeignKey(boolean foreignKey) {
        this.foreignKey = foreignKey;
        return this;
    }

    public ColumnTypeBuilder setIndexed(boolean indexed) {
        this.indexed = indexed;
        return this;
    }

    public ColumnTypeBuilder setAddedFromBaseModelDefinition(boolean addedFromBaseModelDefinition) {
        this.addedFromBaseModelDefinition = addedFromBaseModelDefinition;
        return this;
    }

    public ColumnTypeBuilder setTableRelations(List<TableRelation> tableRelations) {
        this.tableRelations = tableRelations;
        return this;
    }

    public ColumnTypeBuilder setGuidTableRelations() {
        tableRelation.setRelatedColumn(GUID_KEY);
        tableRelations = Collections.singletonList(tableRelation);
        return this;
    }

    public TableColumnType build() {
        TableColumnType columnType = new TableColumnType();
        columnType.setName(name);
        columnType.setType(type);
        columnType.setSize(size);
        columnType.setPrimaryKey(primaryKey);
        columnType.setForeignKey(foreignKey);
        columnType.setIndexed(indexed);
        columnType.setAddedFromBaseModelDefinition(addedFromBaseModelDefinition);
        columnType.setTableRelations(tableRelations);
        return columnType;
    }
}
