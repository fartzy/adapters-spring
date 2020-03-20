package com.acme.ng.provider.adapter.common.service.destination.impl;

import com.acme.ng.provider.adapter.common.model.HouseKeepingFields;
import com.acme.ng.provider.adapter.common.repository.destination.DestinationSystemRepository;
import com.acme.ng.provider.adapter.common.service.columns.TableColumnService;
import com.acme.ng.provider.adapter.common.service.destination.DestinationSystemService;
import com.acme.ng.provider.definition.TableColumnType;
import com.acme.ng.provider.model.ModelInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.acme.ng.provider.adapter.common.model.HouseKeepingFields.*;
import static com.acme.ng.provider.adapter.common.service.columns.TableColumnService.*;

/**
 * 
 * @date 4/16/18
 */
@Service
public class DestinationSystemServiceImpl implements DestinationSystemService {

    private static final Logger LOG = LoggerFactory.getLogger(DestinationSystemServiceImpl.class);

    @Autowired
    private DestinationSystemRepository repository;

    @Autowired
    private TableColumnService columnService;

    @Override
    public ModelInstance getDestinationEntityByPrimaryKeys(ModelInstance entity) {

        List<Object> primaryKeysValues = columnService
                .sortAndExtractNonHousekeepingColumns(entity.getModelDefinition(), PK_COLUMNS_PREDICATE).stream()
                .map(column -> entity.getBusinessData().get(column.getName()))
                .collect(Collectors.toList());


        Map<String, Object> values = repository.selectDestinationEntityByPrimaryKeys(entity.getModelDefinition(), primaryKeysValues);

        if (values.isEmpty()) {
            return null;
        } else {
            return new ModelInstance(entity.getModelDefinition(), values);
        }
    }

    @Override
    public ModelInstance getDestinationEntityByBusinessKeys(ModelInstance entity) {

        Predicate<TableColumnType> columnsPredicate =
                columnService.definitionContainsBKColumns(entity.getModelDefinition()) ? BK_COLUMNS_PREDICATE : PK_COLUMNS_PREDICATE;

        List<Object> businessKeysValues = columnService
                .sortAndExtractNonHousekeepingColumns(entity.getModelDefinition(), columnsPredicate).stream()
                .map(column -> entity.getBusinessData().get(column.getName()))
                .collect(Collectors.toList());

        Map<String, Object> values = repository.selectDestinationEntityByBusinessKeys(entity.getModelDefinition(), businessKeysValues);
        if (!values.isEmpty()) {
            return new ModelInstance(entity.getModelDefinition(), values);
        } else {
            return null;
        }
    }

    @Override
    public void updateDestinationEntityByPrimaryKeys(ModelInstance modelInstance) {
        LOG.debug("updating by Primary Keys; model instance raw {}", modelInstance.getBusinessData());
        populateDestinationHousekeepingFields(modelInstance, "update");

        LOG.debug("updating by Primary Keys; model instance after populating HK fields {}", modelInstance.getBusinessData());
        List<Object> regularColumnsValues = columnService
                .sortAndExtractNonHousekeepingColumns(modelInstance.getModelDefinition(), NON_PK_COLUMNS_PREDICATE).stream()
                .map(column -> modelInstance.getBusinessData().get(column.getName()))
                .collect(Collectors.toList());

        LOG.debug("regularColumnValues {}", regularColumnsValues);

        List<Object> primaryKeysValues = columnService
                .sortAndExtractNonHousekeepingColumns(modelInstance.getModelDefinition(), PK_COLUMNS_PREDICATE).stream()
                .map(column -> modelInstance.getBusinessData().get(column.getName()))
                .collect(Collectors.toList());

        LOG.debug("primaryKeysValues {}", primaryKeysValues);

        repository.updateDestinationEntityByPrimaryKeys(
                modelInstance.getModelDefinition(), primaryKeysValues, regularColumnsValues);
    }

    @Override
    public void saveDestinationEntity(ModelInstance modelInstance) {
        populateDestinationHousekeepingFields(modelInstance, "insert");

        List<Object> allColumnsValues = columnService
                .sortAndExtractNonHousekeepingColumns(modelInstance.getModelDefinition(), ALL_COLUMNS_WITHOUT_THOSE_THAT_ARE_GENERATED_BY_SEQUENCES_PREDICATE).stream()
                .map(column -> modelInstance.getBusinessData().get(column.getName()))
                .collect(Collectors.toList());

        repository.insertDestinationEntity(modelInstance.getModelDefinition(), allColumnsValues);
    }

    // TODO
    // This is a TEMPORARY solution to populate the destination housekeeping fields in lieu of the fact that the
    // models doesn't have the mappings for them in time for the soft launch. This should be removed once the mappings
    // are in place.
    private static void populateDestinationHousekeepingFields(ModelInstance modelInstance, String mode) {
        Timestamp nowStamp = Timestamp.valueOf(LocalDateTime.now());

        Map<String, Object> businessData = modelInstance.getBusinessData();

        modelInstance.getModelDefinition().getTableColumns().forEach(tableColumnType -> {
            String name = tableColumnType.getName();

            if (name.equalsIgnoreCase(LAST_UPDTD_TIMESTMP_FN.getValue())) {
                businessData.put(name, nowStamp);
            } else if (name.equalsIgnoreCase(LAST_UPDTD_BY_FN.getValue())) {
                businessData.put(name, NEXTGEN_UPDATED_CREATED_BY.getValue());
            }

            // Added for DE88849 - only want to populate 'created' fields on insert
            if ("insert".equalsIgnoreCase(mode)) {
                if (name.equalsIgnoreCase(CRETD_TIMESTMP_FN.getValue())) {
                    businessData.putIfAbsent(name, nowStamp);
                } else if ("insert".equalsIgnoreCase(mode) && name.equalsIgnoreCase(CRETD_BY_FN.getValue())) {
                    businessData.putIfAbsent(name, NEXTGEN_UPDATED_CREATED_BY.getValue());
                }
            }
        });
    }
}
