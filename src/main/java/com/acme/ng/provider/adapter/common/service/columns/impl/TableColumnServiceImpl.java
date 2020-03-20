package com.acme.ng.provider.adapter.common.service.columns.impl;

import com.acme.ng.provider.adapter.common.service.columns.TableColumnService;
import com.acme.ng.provider.definition.TableColumnType;
import com.acme.ng.provider.model.GlobalConstants;
import com.acme.ng.provider.model.common.ModelDefinition;
import com.acme.ng.provider.util.ModelDefUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Alexander Lukyanchikov
 * @date 4/11/18
 */
@Service
public class TableColumnServiceImpl implements TableColumnService {

    @Override
    public List<TableColumnType> sortAndExtractNonHousekeepingColumns(ModelDefinition modelDefinition, Predicate<TableColumnType> predicate) {
        return modelDefinition.getTableColumns().stream()
                .sorted(Comparator.comparing(TableColumnType::getName))
                .filter(column -> !column.isAddedFromBaseModelDefinition())
                .filter(column -> !(column.isForeignKey() && ModelDefUtils.effectiveTableRelations(column)
                        .stream()
                        .anyMatch(rel -> Objects.equals(rel.getRelatedColumn(), GlobalConstants.GUID_KEY))))
                .filter(predicate)
                .collect(Collectors.toList());
    } // may use  condition (|| column.getName().toUpperCase().contains(GlobalConstants.GUID_KEY.toUpperCase()))

    @Override
    public boolean definitionContainsBKColumns(ModelDefinition modelDefinition) {
        return modelDefinition.getTableColumns().stream()
                .anyMatch(TableColumnType::isBusinessKey);
    }
}
