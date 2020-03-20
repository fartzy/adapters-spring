package com.acme.ng.provider.adapter.common.service.impl;

import com.acme.ng.provider.adapter.common.Utils;
import com.acme.ng.provider.adapter.common.domain.SyncResult;
import com.acme.ng.provider.adapter.common.service.ExternalDbSyncService;
import com.acme.ng.provider.adapter.common.service.ModelInstanceReassembler;
import com.acme.ng.provider.adapter.common.service.destination.ComparisonResult;
import com.acme.ng.provider.adapter.common.service.destination.DestinationComparison;
import com.acme.ng.provider.adapter.common.service.destination.DestinationComparisonUtils;
import com.acme.ng.provider.adapter.common.service.destination.DestinationSystemService;
import com.acme.ng.provider.common.repository.document.definitions.ModelDefinitionRepository;
import com.acme.ng.provider.core.exception.ComponentException;
import com.acme.ng.provider.definition.TableColumnType;
import com.acme.ng.provider.model.GlobalConstants;
import com.acme.ng.provider.model.ModelInstance;
import com.acme.ng.provider.model.adapter.AdapterEvent;
import com.acme.ng.provider.model.common.ModelDefinition;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

import static com.acme.ng.provider.adapter.common.model.HouseKeepingFields.*;
import static java.util.stream.Collectors.toSet;

/**
 * Created  4/26/2018.
 */
@Component
public class ExternalDbSyncServiceImpl implements ExternalDbSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalDbSyncServiceImpl.class);

    @Autowired
    private DestinationSystemService destinationSystemService;

    @Autowired
    private ModelDefinitionRepository modelDefinitionRepository;

    @Autowired
    private DestinationComparison destinationComparison;

    @Autowired
    private DestinationComparisonUtils destinationComparisonUtils;

    @Autowired
    private ModelInstanceReassembler modelInstanceReassembler;

    @Value("${adapter.performInstanceComparison}") //set to false to skip compare logic. default is true
    private boolean performInstanceComparison;


    @Override
    public SyncResult sync(AdapterEvent adapterEvent) {
        ModelInstance payloadInstanceSnapshot =
                modelInstanceReassembler.reassembleModelInstance(adapterEvent.getModelInstanceSnapshot());
        ModelInstance previousInstanceSnapshot = adapterEvent.getPreviousInstanceSnapshot() == null ? null :
                modelInstanceReassembler.reassembleModelInstance(adapterEvent.getPreviousInstanceSnapshot());

        boolean performCompare = !adapterEvent.isOverrideComparisonAtDestination() && performInstanceComparison;

        LOG.debug("Start syncing to destination. Model Instance with GUID: {}", payloadInstanceSnapshot.getId());

        checkIfGuidAndDestinationForeignKeysArePopulated(payloadInstanceSnapshot);

        SyncResult syncResult;
        if (hasPrimaryKeysPresentAndPopulated(payloadInstanceSnapshot)) {
            syncResult = syncWithPrimaryKeysPresentAndPopulated(payloadInstanceSnapshot, previousInstanceSnapshot,
                    adapterEvent, performCompare);
        } else {
            syncResult = syncWithoutPrimaryKeysPresentAndPopulated(payloadInstanceSnapshot, previousInstanceSnapshot,
                    adapterEvent, performCompare);
        }

        return syncResult;
    }

    private SyncResult syncWithPrimaryKeysPresentAndPopulated(ModelInstance payloadInstanceSnapshot,
                                                              ModelInstance previousInstanceSnapshot,
                                                              AdapterEvent adapterEvent, boolean performCompare) {
        SyncResult syncResult = null;

        // process record for table like "BUS_ENTTY_TY" with already predefined primary key
        // it can be persisted earlier or NOT
        ModelInstance destinationEntityByPrimaryKeys =
                destinationSystemService.getDestinationEntityByPrimaryKeys(payloadInstanceSnapshot);

        if (performCompare) {
            ComparisonResult comparisonResult =
                    destinationComparison.compareToDestination(destinationEntityByPrimaryKeys, payloadInstanceSnapshot,
                            previousInstanceSnapshot);
            if (comparisonResult.isNotExecutedUnacceptable() || comparisonResult.isFailure()) {
                prepareAdapterEventForComponentException(comparisonResult, adapterEvent, destinationEntityByPrimaryKeys);
                throw createComponentException(comparisonResult, payloadInstanceSnapshot, adapterEvent);
            } else if (comparisonResult.isSuccess() && comparisonResult.isComparedAgainstPersistingInstance()) {
                /* No previousInstanceSnapshot, so payload is compared to destination and there are no differences
                   update HK fields and  return sync*/
                ModelInstance enrichedPayloadInstanceSnapshot = enrichPayloadModelInstanceWithHouseKeepingFields(payloadInstanceSnapshot, destinationEntityByPrimaryKeys);
                syncResult = new SyncResult(enrichedPayloadInstanceSnapshot, Collections.emptyMap());
            }
        }

        if (syncResult == null) {
            if (!isPrimaryKeysHaveSequenceName(payloadInstanceSnapshot)) {
                syncResult =
                        performSyncForPrimaryKeyWithSequenceName(destinationEntityByPrimaryKeys, payloadInstanceSnapshot, previousInstanceSnapshot);
            } else {
                syncResult = performSyncForPrimaryKeyWithoutSequenceName(destinationEntityByPrimaryKeys, payloadInstanceSnapshot);
            }
        }

        return syncResult;
    }

    private SyncResult performSyncForPrimaryKeyWithSequenceName(ModelInstance destinationEntityByPrimaryKeys,
                                                                ModelInstance payloadInstanceSnapshot,
                                                                ModelInstance previousInstanceSnapshot) {


        SyncResult syncResult;

        if (destinationEntityByPrimaryKeys == null) {
            syncResult = performInsert(payloadInstanceSnapshot);
        } else {

            // destinationEntityByPrimaryKeys contains the data on the destination database prior to the update
            // payloadInstanceSnapshot contains the (updated) data from the Consumer layer that will be applied to the destination database

            // capure 'created' Housekeeping fields from previous version and apply to new verison - DE88849
            payloadInstanceSnapshot.getBusinessData().put(CRETD_TIMESTMP_FN.getValue(), destinationEntityByPrimaryKeys.getBusinessData().get(CRETD_TIMESTMP_FN.getValue()));
            payloadInstanceSnapshot.getBusinessData().put(CRETD_BY_FN.getValue(), destinationEntityByPrimaryKeys.getBusinessData().get(CRETD_BY_FN.getValue()));

            destinationSystemService.updateDestinationEntityByPrimaryKeys(payloadInstanceSnapshot);

            Map<String, Object> changedFields = getChangedFields(payloadInstanceSnapshot, destinationEntityByPrimaryKeys);

            syncResult = new SyncResult(payloadInstanceSnapshot, changedFields);
        }

        return syncResult;
    }

    private SyncResult performSyncForPrimaryKeyWithoutSequenceName(ModelInstance destinationEntityByPrimaryKeys,
                                                                   ModelInstance payloadInstanceSnapshot) {
        // destinationEntityByPrimaryKeys contains the data on the destination database prior to the update
        // payloadInstanceSnapshot contains the (updated) data from the Consumer layer that will be applied to the destination database


        // capure 'created' Housekeeping fields from previous version and apply to new verison - DE88849
        payloadInstanceSnapshot.getBusinessData().put(CRETD_TIMESTMP_FN.getValue(), destinationEntityByPrimaryKeys.getBusinessData().get(CRETD_TIMESTMP_FN.getValue()));
        payloadInstanceSnapshot.getBusinessData().put(CRETD_BY_FN.getValue(), destinationEntityByPrimaryKeys.getBusinessData().get(CRETD_BY_FN.getValue()));

        destinationSystemService.updateDestinationEntityByPrimaryKeys(payloadInstanceSnapshot);


        Map<String, Object> changedFields = getChangedFields(payloadInstanceSnapshot, destinationEntityByPrimaryKeys);

        LOG.debug("Changed fields for Model Instance with GUID: {} retrieved. Total: {} changes",
                payloadInstanceSnapshot.getId(), changedFields.size());

        return new SyncResult(payloadInstanceSnapshot, changedFields);
    }

    private SyncResult syncWithoutPrimaryKeysPresentAndPopulated(ModelInstance payloadInstanceSnapshot,
                                                                 ModelInstance previousInstanceSnapshot,
                                                                 AdapterEvent adapterEvent, boolean performCompare) {
        SyncResult syncResult = null;

        ModelInstance destinationEntityByBusinessKeys =
                destinationSystemService.getDestinationEntityByBusinessKeys(payloadInstanceSnapshot);
        if (performCompare) {
            ComparisonResult comparisonResult =
                    destinationComparison.compareToDestination(destinationEntityByBusinessKeys, payloadInstanceSnapshot,
                            previousInstanceSnapshot);
            if (comparisonResult.isNotExecutedUnacceptable() || comparisonResult.isFailure()) {
                prepareAdapterEventForComponentException(comparisonResult, adapterEvent, destinationEntityByBusinessKeys);
                throw createComponentException(comparisonResult, payloadInstanceSnapshot, adapterEvent);
            } else if (comparisonResult.isSuccess() && comparisonResult.isComparedAgainstPersistingInstance()) {
                /* No previousInstanceSnapshot, so payload is compared to destination and there are no differences
                   update HK Fields return sync*/
                ModelInstance enrichedPayloadInstanceSnapshot = enrichPayloadModelInstanceWithHouseKeepingFields(payloadInstanceSnapshot, destinationEntityByBusinessKeys);


                Map<String, Object> changedFields = new HashMap<>();
                final ModelDefinition modelDefinition =
                        modelDefinitionRepository.findByDefName(enrichedPayloadInstanceSnapshot.getType());
                modelDefinition.getTableColumns().stream()
                        .filter(t -> t.isPrimaryKey() || t.isForeignKey() || t.isSoftRelation())
                        .filter(t -> !t.getName().equals(GlobalConstants.GUID_KEY))
                        .map(TableColumnType::getName)
                        .forEach(column -> changedFields.put(column, destinationEntityByBusinessKeys.get(column)));

                syncResult = new SyncResult(enrichedPayloadInstanceSnapshot, changedFields);
            }
        }

        if (syncResult == null) {
            if (destinationEntityByBusinessKeys != null) {
                updatePrimaryKeysOnPayloadInstanceSnapshot(payloadInstanceSnapshot, destinationEntityByBusinessKeys);
                syncResult = performUpdateWithBusinessKeys(destinationEntityByBusinessKeys, payloadInstanceSnapshot, adapterEvent.getPreviousInstanceSnapshot());
            } else {
                syncResult = performInsert(payloadInstanceSnapshot);
            }
        }

        return syncResult;
    }

    private void updatePrimaryKeysOnPayloadInstanceSnapshot(ModelInstance payloadInstanceSnapshot, ModelInstance destinationEntityByBusinessKeys) {
        ModelDefinition modelDefinition = payloadInstanceSnapshot.getModelDefinition();

        Set<String> primaryKeyColumnNames = Utils.getPrimaryKeyColumnNames(modelDefinition);

        Map<String, Object> primaryKeysAndColumnsOfTheDestinationEntityByBusinessKeys = Utils.getColumnNamesAndValues(destinationEntityByBusinessKeys, primaryKeyColumnNames);

        for (Map.Entry<String, Object> entry : primaryKeysAndColumnsOfTheDestinationEntityByBusinessKeys.entrySet()) {
            if (GlobalConstants.GUID_KEY.equals(entry.getKey())) {
                continue;
            }
            if (!Objects.equals(entry.getValue(), payloadInstanceSnapshot.getBusinessData().get(entry.getKey()))) {
                //throw new RuntimeException("Found a record by business key, but primary key is not the same");
                payloadInstanceSnapshot.getBusinessData().put(entry.getKey(),entry.getValue());

            }
        }
    }

    private SyncResult performUpdateWithBusinessKeys(ModelInstance destinationEntityByBusinessKeys,
                                                     ModelInstance payloadInstanceSnapshot,
                                                     ModelInstance previousInstanceSnapshot) {

        // destinationEntityByBusinessKeys contains the data on the destination database prior to the update
        // payloadInstanceSnapshot contains the (updated) data that will be applied to the destination database

        // capure 'created' Housekeeping fields from previous version and apply to new verison - DE88849
        payloadInstanceSnapshot.getBusinessData().put(CRETD_TIMESTMP_FN.getValue(), destinationEntityByBusinessKeys.getBusinessData().get(CRETD_TIMESTMP_FN.getValue()));
        payloadInstanceSnapshot.getBusinessData().put(CRETD_BY_FN.getValue(), destinationEntityByBusinessKeys.getBusinessData().get(CRETD_BY_FN.getValue()));

        Map<String, Object> changedFields = getChangedFields(payloadInstanceSnapshot, destinationEntityByBusinessKeys);

        destinationSystemService.updateDestinationEntityByPrimaryKeys(payloadInstanceSnapshot);


        return new SyncResult(payloadInstanceSnapshot, changedFields);
    }

    private ComponentException createComponentException(ComparisonResult comparisonResult,
                                                        ModelInstance payloadInstanceSnapshot, AdapterEvent adapterEvent) {
        String message = destinationComparisonUtils.buildCompleteComparisonResultMessage(comparisonResult);
        return new ComponentException(message, payloadInstanceSnapshot.getType(), payloadInstanceSnapshot.getId(),
                adapterEvent, true);
    }

    private SyncResult performInsert(ModelInstance payloadModelInstance) {
        destinationSystemService.saveDestinationEntity(payloadModelInstance);
        ModelInstance newlyCreatedModelInstance =
                destinationSystemService.getDestinationEntityByBusinessKeys(payloadModelInstance);
        if (newlyCreatedModelInstance == null) {
            throw new IllegalStateException("An error occurred during adapter sync - failed to find just stored Model Instance from db by business key");
        }
        Map<String, Object> changedFields = getChangedFields(payloadModelInstance, newlyCreatedModelInstance);

        if (changedFields.isEmpty()) { // means that record for table like "BUS_ENTTY_TY" was inserted
            changedFields = payloadModelInstance.getBusinessData();
        }

        //TODO DONE: instead of "newlyCreatedModelInstance" we need to pass "input". Input need to have all (except guid) primary fields from "newlyCreatedModelInstance"
        ModelInstance enrichedPayloadModelInstance =
                enrichPayloadModelInstanceWithPrimaryKeysFields(payloadModelInstance, newlyCreatedModelInstance);

        return new SyncResult(enrichedPayloadModelInstance, changedFields);
    }

    //TODO DONE: if FK guid is present, check for destination FK for that, also change the method name
    private static void checkIfGuidAndDestinationForeignKeysArePopulated(ModelInstance payloadModelInstance) {
        Map<String, Set<TableColumnType>> foreignKeysGatheredByRelatedTable =
                Utils.getForeignKeysGatheredByRelatedTable(payloadModelInstance.getModelDefinition());

        for (Map.Entry<String, Set<TableColumnType>> foreignKeysSet : foreignKeysGatheredByRelatedTable.entrySet()) {
            String foreignKeyGuidColumnName = Utils.getGuidForeignKeyName(foreignKeysSet.getValue());
            Set<String> foreignKeyNonGuidColumnNames = Utils.getNonGuidForeignKeyNames(foreignKeysSet.getValue());

            if (payloadModelInstance.get(foreignKeyGuidColumnName) != null) {
                long countOfNullNonGuidColumnNames =
                        foreignKeyNonGuidColumnNames.stream()
                                .filter(foreignKeyNonGuidColumnName ->
                                        payloadModelInstance.get(foreignKeyNonGuidColumnName) == null)
                                .count();
                if (countOfNullNonGuidColumnNames > 0) {
                    throw new IllegalStateException("An error occurred during adapter sync - destination foreign key should be populated on the payload, but is NULL");
                }
            }
        }
    }

    private static boolean hasPrimaryKeysPresentAndPopulated(ModelInstance payloadInstanceSnapshot) {
        boolean primaryKeysPresent = checkIfDestinationPrimaryKeysPresentInDefinition(payloadInstanceSnapshot);

        boolean primaryKeysPopulated = false;

        if (primaryKeysPresent) {
            primaryKeysPopulated = checkIfAnyDestinationPrimaryKeysPopulatedOnInstance(payloadInstanceSnapshot);
        }

        return primaryKeysPresent && primaryKeysPopulated;
    }

    private static boolean checkIfDestinationPrimaryKeysPresentInDefinition(ModelInstance payloadModelInstance) {
        Set<String> primaryKeyColumnNames =
                Utils.getDestinationPrimaryKeyColumnNames(payloadModelInstance.getModelDefinition());

        return CollectionUtils.isNotEmpty(primaryKeyColumnNames);
    }

    private static boolean checkIfAnyDestinationPrimaryKeysPopulatedOnInstance(ModelInstance payloadModelInstance) {
        Set<String> primaryKeyColumnNames =
                Utils.getDestinationPrimaryKeyColumnNames(payloadModelInstance.getModelDefinition());

        return primaryKeyColumnNames.stream()
                .allMatch(primaryKeyColumnName -> payloadModelInstance.getBusinessData().get(primaryKeyColumnName) != null);
    }

    private static void prepareAdapterEventForComponentException(ComparisonResult comparisonResult, AdapterEvent adapterEvent,
                                                                 ModelInstance destinationInstance) {
        List<String> failedFieldNames =
                comparisonResult.getFieldComparisonFailures().stream()
                        .map(fieldComparisonFailure -> fieldComparisonFailure.getTableColumnType().getName())
                        .collect(Collectors.toList());
        adapterEvent.setCompareMismatchAttributes(failedFieldNames);
        adapterEvent.setDestinationInstanceSnapshot(destinationInstance);
    }

    private static boolean isPrimaryKeysHaveSequenceName(ModelInstance payloadModelInstance) {
        Set<TableColumnType> primaryKeyColumnTypes =
                getDestinationPrimaryKeyColumnTypes(payloadModelInstance.getModelDefinition());

        Assert.isTrue(CollectionUtils.isNotEmpty(primaryKeyColumnTypes), "At least one primary key has to be present.");

        return primaryKeyColumnTypes.stream()
                .allMatch(primaryKeyColumnType -> Strings.isNotEmpty(primaryKeyColumnType.getSequenceName()));
    }

    private static ModelInstance enrichPayloadModelInstanceWithPrimaryKeysFields(ModelInstance payloadModelInstance,
                                                                                 ModelInstance newlyCreatedModelInstance) {

        Map<String, Object> destinationEntity = newlyCreatedModelInstance.getBusinessData();

        Set<String> destinationPrimaryKeyColumnNames =
                Utils.getDestinationPrimaryKeyColumnNames(payloadModelInstance.getModelDefinition());

        destinationPrimaryKeyColumnNames.add(LAST_UPDTD_TIMESTMP_FN.getValue());
        destinationPrimaryKeyColumnNames.add(LAST_UPDTD_BY_FN.getValue());
        destinationPrimaryKeyColumnNames.add(CRETD_TIMESTMP_FN.getValue());
        destinationPrimaryKeyColumnNames.add(CRETD_BY_FN.getValue());


        for (String destinationPrimaryKeyColumnName : destinationPrimaryKeyColumnNames) {
            payloadModelInstance.put(destinationPrimaryKeyColumnName,
                    destinationEntity.get(destinationPrimaryKeyColumnName));
        }

        return new ModelInstance(payloadModelInstance.getModelDefinition(), payloadModelInstance.getBusinessData());
    }

    private static ModelInstance enrichPayloadModelInstanceWithHouseKeepingFields(ModelInstance payloadModelInstance,
                                                                                  ModelInstance destinationModelInstance) {
        /* DE91023 - if previous instance is null, then this is the first time Publisher has seen this entity;
           However the entity already exists on the destination, indicating this is a migration/full refresh
           scenario.  Must pull the HouseKeeping Fields from destination and set insertedInDestinationDB to true
           in order to make it look like the data in the destination originally came from publisher
        */

        Map<String, Object> destinationEntity = destinationModelInstance.getBusinessData();
        payloadModelInstance.getBusinessData().put(CRETD_TIMESTMP_FN.getValue(), destinationModelInstance.getBusinessData().get(CRETD_TIMESTMP_FN.getValue()));
        payloadModelInstance.getBusinessData().put(CRETD_BY_FN.getValue(), destinationModelInstance.getBusinessData().get(CRETD_BY_FN.getValue()));
        payloadModelInstance.getBusinessData().put(LAST_UPDTD_TIMESTMP_FN.getValue(), destinationModelInstance.getBusinessData().get(LAST_UPDTD_TIMESTMP_FN.getValue()));
        payloadModelInstance.getBusinessData().put(LAST_UPDTD_BY_FN.getValue(), destinationModelInstance.getBusinessData().get(LAST_UPDTD_BY_FN.getValue()));

        return new ModelInstance(payloadModelInstance.getModelDefinition(), payloadModelInstance.getBusinessData());

    }

    private static Set<TableColumnType> getDestinationPrimaryKeyColumnTypes(ModelDefinition modelDefinition) {
        return modelDefinition.getTableColumns().stream()
                .filter(TableColumnType::isPrimaryKey)
                .filter(tableColumnType -> !tableColumnType.getName().equals(GlobalConstants.GUID_KEY))
                .collect(toSet());
    }

    /**
     * returns changed fields from destination layer, so contains no GUID fields
     *
     * @param payloadModelInstance
     * @param destinationModelInstance
     * @return
     */
    private static Map<String, Object> getChangedFields(ModelInstance payloadModelInstance,
                                                        ModelInstance destinationModelInstance) {

        Map<String, Object> changedFields = new HashMap<>();

        if (destinationModelInstance == null) {
            //TODO DONE: build "changedFields" without primary and foreign GUIDs and without housekeeping fields

            changedFields = new HashMap<>(payloadModelInstance.getBusinessData());

            Set<String> primaryAndForeignGuidsAndHousekeepingFieldNames =
                    Utils.getPrimaryAndForeignGuidsAndHousekeepingFieldNames(payloadModelInstance.getModelDefinition());

            for (String fieldName : primaryAndForeignGuidsAndHousekeepingFieldNames) {
                changedFields.remove(fieldName);
            }

            //TODO DONE: if "destinationModelInstance" is null or empty (happens when INSERT a new entity to destination) then return all the fields from "payloadModelInstance"
        } else {
            Map<String, Object> inputModelInstanceFields =
//                    Utils.copyBusinessDataWithLowerCaseKeys(payloadModelInstance.getBusinessData());
                    Utils.copyBusinessData(payloadModelInstance.getBusinessData());
            Map<String, Object> changedEntity =
//                    Utils.copyBusinessDataWithLowerCaseKeys(destinationModelInstance.getBusinessData());
                    Utils.copyBusinessData(destinationModelInstance.getBusinessData());

            for (Map.Entry<String, Object> entry : changedEntity.entrySet()) {
                if (!Objects.equals(entry.getValue(), inputModelInstanceFields.get(entry.getKey()))) {
                    changedFields.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return changedFields;
    }
}
