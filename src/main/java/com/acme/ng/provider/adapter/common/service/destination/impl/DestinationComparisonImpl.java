package com.acme.ng.provider.adapter.common.service.destination.impl;

import com.acme.ng.provider.adapter.common.Utils;
import com.acme.ng.provider.adapter.common.service.destination.ComparisonResult;
import com.acme.ng.provider.adapter.common.service.destination.ComparisonResult.ComparisonResultType;
import com.acme.ng.provider.adapter.common.service.destination.DestinationComparison;
import com.acme.ng.provider.adapter.common.service.destination.FieldComparisonFailure;
import com.acme.ng.provider.common.service.ColumnTypeConvertService;
import com.acme.ng.provider.common.service.impl.ColumnTypeConvertServiceImpl;
import com.acme.ng.provider.definition.TableColumnType;
import com.acme.ng.provider.model.ModelInstance;
import com.acme.ng.provider.model.common.ModelDefinition;
import com.acme.ng.provider.util.ModelDefUtils;
import com.mongodb.Mongo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.br.CPF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.ibm.db2.jcc.am.ib.of;
import static com.sun.org.apache.xalan.internal.xsltc.compiler.sym.error;
import static org.apache.commons.lang3.ObjectUtils.compare;
import static org.apache.coyote.http11.Constants.a;
import static org.springframework.util.ObjectUtils.nullSafeEquals;

/**
 * This class is the primary implementation of the DestinationComparison interface
     * Created by Julian M on 10/3/18.
 */
@Service
public class DestinationComparisonImpl implements DestinationComparison {
    private static final Logger LOG = LoggerFactory.getLogger(DestinationComparisonImpl.class);

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static final String MSG_NO_PREVIOUS_INSTANCE_FAILURE =
            "No previous entity in AdapterEvent and current entity does not match destination entity";
    private static final String MSG_PREVIOUS_INSTANCE_FAILURE =
            "Previous entity in AdapterEvent does not match destination entity";
    static final String MSG_NO_DESTINATION_INSTANCE_FAILURE =
            "Manual Review Required - No destination record but previous record found on AdapterEvent";


    static final MutableComparisonResult RESULT_NO_DEST_OR_PREV =
            new MutableComparisonResult(null, null, ComparisonResultType.NOT_EXECUTED_ACCEPTABLE);

    @Autowired
    private ColumnTypeConvertService columnTypeConvertService;

    @Override
    public ComparisonResult compareToDestination(ModelInstance destinationInstance, ModelInstance persistingInstance,
                                                 ModelInstance previousInstance) {
        Objects.requireNonNull(persistingInstance, "persisting instance cannot be null");

        MutableComparisonResult comparisonResult;

        if (destinationInstance == null) {
            comparisonResult = handleNoDestinationInstance(previousInstance);
        } else {
            // if we don't have a previous instance then we will compare the destination to the instance we want to
            // persist, otherwise we will use the previous instance.
            if (previousInstance == null) {
                comparisonResult =
                        executeComparison(destinationInstance, persistingInstance, MSG_NO_PREVIOUS_INSTANCE_FAILURE);
                comparisonResult.setComparedAgainstPersistingInstance(true);
            } else {
                comparisonResult =
                        executeComparison(destinationInstance, previousInstance, MSG_PREVIOUS_INSTANCE_FAILURE);
            }

            if (comparisonResult.isFailure() || comparisonResult.isNotExecutedUnacceptable()) {
                LOG.error(comparisonResult.toString());
            }
        }

        return comparisonResult;
    }

    private static MutableComparisonResult handleNoDestinationInstance(ModelInstance previousInstance) {
        MutableComparisonResult comparisonResult;

        if (previousInstance == null) {
            // there is nothing to do if we have neither a destination nor a previous instance
            comparisonResult = RESULT_NO_DEST_OR_PREV;
        } else {
            // if we don't have a destination but we do have a previous instance something is out of sync. we can't do
            // a comparison but this not an expected situation.
            LOG.error("Manual Review Required - No destination record but previous record found on AdapterEvent: {}",
                    previousInstance);
            comparisonResult =
                    new MutableComparisonResult(previousInstance, null, ComparisonResultType.NOT_EXECUTED_UNACCEPTABLE)
                    .setMessage(MSG_NO_DESTINATION_INSTANCE_FAILURE);
        }

        return comparisonResult;
    }

    private MutableComparisonResult executeComparison(ModelInstance destinationInstance, ModelInstance compareToInstance,
                                     String failureMessage) {
        ModelDefinition modelDefinition = compareToInstance.getModelDefinition();
        assertModelDefinition(modelDefinition);

        List<String> comparableColumnNames = ModelDefUtils.getComparableColumnNames(modelDefinition);

        MutableComparisonResult comparisonResult;
        if (CollectionUtils.isEmpty(comparableColumnNames)) {
            // it is possible that a table is defined with no comparable columns. if this is the case we can't do a
            // comparison so just report back that the comparison was not executed. this is an expected scenario.
            comparisonResult = new MutableComparisonResult(compareToInstance, destinationInstance,
                    ComparisonResultType.NOT_EXECUTED_UNACCEPTABLE);
        } else {
            // we have everything we need to actually start the comparison
            comparisonResult = compareInstances(destinationInstance, compareToInstance, failureMessage,
                    modelDefinition, comparableColumnNames);
        }

        return comparisonResult;
    }

    private MutableComparisonResult compareInstances(ModelInstance destinationInstance, ModelInstance compareToInstance,
                                              String failureMessage, ModelDefinition modelDefinition, List<String> comparableColumnNames) {
        List<FieldComparisonFailure> fieldComparisonFailures = new ArrayList<>();

        Map<String, Object> destinationBusinessData =
                Utils.copyBusinessDataWithLowerCaseKeys(destinationInstance.getBusinessData());
        Map<String, Object> compareToBusinessData =
                Utils.copyBusinessDataWithLowerCaseKeys(compareToInstance.getBusinessData());

        // we will compare each comparable column. if the comparison fails we will add it to our list
        comparableColumnNames.forEach(columnName -> {
            Optional<FieldComparisonFailure> optionalFailure = compareColumn(columnName.toLowerCase(),
                    destinationBusinessData, compareToBusinessData, modelDefinition);
            optionalFailure.ifPresent(fieldComparisonFailures::add);
        });

        // create the final comparison result
        return createComparisonExecutedComparisonResult(fieldComparisonFailures, destinationInstance,
                compareToInstance, failureMessage, comparableColumnNames);
    }

    private Optional<FieldComparisonFailure> compareColumn(String lowerCaseColumnName, Map<String,
            Object> destinationBusinessData, Map<String, Object> compareToBusinessData,
                                                           ModelDefinition modelDefinition) {
        FieldComparisonFailure fieldComparisonFailure = null;

        Object compareToFieldValue = compareToBusinessData.get(lowerCaseColumnName);
        Object destinationFieldValue = destinationBusinessData.get(lowerCaseColumnName);

        Optional<TableColumnType> optionalTableColumnType = modelDefinition.getTableColumns().stream()
                .filter(tableColumnType -> lowerCaseColumnName.equalsIgnoreCase(tableColumnType.getName()))
                .findFirst();

        if (optionalTableColumnType.isPresent()
                && canCompare(optionalTableColumnType.get(), compareToFieldValue)
                && !compareValues(destinationFieldValue, compareToFieldValue,  optionalTableColumnType.get(),
                        modelDefinition.getDefID())) {
            fieldComparisonFailure = new FieldComparisonFailure(optionalTableColumnType.get(),
                    destinationFieldValue, compareToFieldValue);
        }

        return Optional.ofNullable(fieldComparisonFailure);
    }

    private boolean compareValues(Object destinationFieldValue, Object compareToFieldValue,
                                  TableColumnType tableColumnType, String modelDefinitionId) {
        // ensure both the destination and compareTo data are consistent data types based on the CONSUMER column
        // definition.
        Object destinationFieldValueToCompare =
                columnTypeConvertService.convert(preProcessValue(destinationFieldValue), tableColumnType,
                        modelDefinitionId);
        Object compareToFieldValueToCompare =
                columnTypeConvertService.convert(preProcessValue(compareToFieldValue), tableColumnType,
                        modelDefinitionId);

        // compare both values checking for equality except in the case of BigDecimal. For BigDecimal use Comparable
        // instead so that precision and scale will taken into account. For reference see the following:
        // https://www.journaldev.com/16409/java-bigdecimal#java-bigdecimal-compareto
        boolean result;
        if (destinationFieldValueToCompare instanceof BigDecimal &&
                compareToFieldValueToCompare instanceof  BigDecimal) {
            result = compare((BigDecimal) destinationFieldValueToCompare, (BigDecimal) compareToFieldValueToCompare) == 0;
        } else {
            result = nullSafeEquals(destinationFieldValueToCompare, compareToFieldValueToCompare);
        }

        return result;
    }

    private static boolean canCompare(TableColumnType tableColumnType, Object compareToFieldValue) {
        boolean canCompare = true;

        // if this is an auto-generating destination primary key with an existing null value, we should skip the
        // comparison
        if (tableColumnType != null
            && tableColumnType.isPrimaryKey()
            && StringUtils.isNotBlank(tableColumnType.getSequenceName())
            && compareToFieldValue == null) {
            canCompare = false;
        }

        return canCompare;
    }

    private static Object preProcessValue(Object value) {
         /*if this is a java.sql.Date or a java.time.LocalDate convert it to an Instant using the atStartOfDay capability
         if this is any other type of java.util.Date convert it to a java.time.Instant
         these conversions are so that it can be properly be converted for the purposes of comparing*/
        if (value instanceof java.sql.Date) {
            value = ((java.sql.Date) value).toLocalDate().atStartOfDay(UTC).toInstant();
        } else if (value instanceof java.sql.Timestamp) {
            value = ((java.sql.Timestamp) value).toLocalDateTime().toInstant(ZoneOffset.UTC);
        } else if (value instanceof java.util.Date) {
            value = ((java.util.Date) value).toInstant();
        } else if (value instanceof LocalDate) {
            value = ((LocalDate) value).atStartOfDay(UTC).toInstant();
        } else if (value instanceof String) {
            /*For CHAR fields that are initialized to " " (i.e., space) as default value, the RDMS will actually store
            the full length of the field as spaces.   For instance, if the column is defined as CHAR(65) and in the
            consumer layer the attribute in the Mongo collection is initialized as " ", the value on the backend
            database will really be 65 spaces.   Note if the transformation context initializes the field to " ",
            Mongo will actually store as "" (i.e, the empty string) in the model insteances.  Regardless when comparing
            the exisitng value on the database (65 spaces) to what is in the model instances in Mongo (""), there will
            be a mismatch.  Thus we need to remove trailing spaces*/
            value = StringUtils.replaceFirst((String)value , "\\s++$", "");

            /*Then in CPF if column is  defined as not null with default, we cannot pass in null as value in INSERT statement (get
            SQL error).  So in transformation contexts we default these fields to spaces.  However Mongo stores as an empty
            string (i.e., "").   Then when the previous instance is built in DispatchServiceImpl.preparePreviousData(), values
            for empty strings are converted to nulls (see ColumnTypeConvertServiceImpl.convertModelInstanceValues() and
            ColumnTypeConvertServiceImpl.convert()).  Thus when comparing any Mongo model instance to the previous
            destination instance (i.e, data on back end database), we get a mismatch as null <> " ".  Note in by the time
            the compare is done, the " " values on the previous destination instance actually have a value of "" due to the
            above line of code that removes leading spaces.  However null still does not equal "" and we still have mismatch.
            Thus code is added to set both the destinationFieldValue and the existingFieldValue to null if spaces or empty string.*/
            value = StringUtils.isBlank((String)value) ? null : value;
        }

        return value;
    }

    private static MutableComparisonResult createComparisonExecutedComparisonResult(List<FieldComparisonFailure> fieldComparisonFailures,
                                                                      ModelInstance destinationInstance,
                                                                      ModelInstance compareToInstance,
                                                                      String failureMessage,
                                                                      List<String> comparableColumnNames) {
        MutableComparisonResult comparisonResult;

        // for a comparison it either succeeds or fails. if we have field comparison failures it is a failure.
        if (CollectionUtils.isEmpty(fieldComparisonFailures)) {
            LOG.info("Instance being processed [{}] and destination instance [{}] are equal in value by comparable columns {}",
                    compareToInstance, destinationInstance, comparableColumnNames);
            comparisonResult =
                    new MutableComparisonResult(compareToInstance, destinationInstance, ComparisonResultType.SUCCESS);
        } else {
            comparisonResult =
                    new MutableComparisonResult(compareToInstance, destinationInstance, fieldComparisonFailures)
                    .setComparisonResultType(ComparisonResultType.FAILURE)
                    .setMessage(failureMessage);
        }

        return comparisonResult;
    }

    private static void assertModelDefinition(ModelDefinition modelDefinition) {
        String modelDefErrorMsg =
                String.format("Model definition id=%s, defName=%s should contain at least one table column defined",
                        modelDefinition.getDefID(),  modelDefinition.getDefName());
        Assert.isTrue(CollectionUtils.isNotEmpty(modelDefinition.getTableColumns()), modelDefErrorMsg);
    }
}
