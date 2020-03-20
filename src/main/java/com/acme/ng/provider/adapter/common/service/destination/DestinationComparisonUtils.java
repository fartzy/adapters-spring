package com.acme.ng.provider.adapter.common.service.destination;

import com.acme.ng.provider.adapter.common.Utils;
import com.acme.ng.provider.adapter.common.service.columns.TableColumnService;
import com.acme.ng.provider.model.ModelInstance;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class provides utility methods related to comparing against destination data
 * Created by Julian M on 10/4/18.
 */
@Service
public class DestinationComparisonUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DestinationComparisonUtils.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");

    @Autowired
    private TableColumnService tableColumnService;

    /**
     * Builds the string representation of the ComparisonResult in a very complete format suitable for display to an
     * end user
     *
     * @param comparisonResult The results of a comparison against the destination
     * @return String
     */
    public String buildCompleteComparisonResultMessage(ComparisonResult comparisonResult) {
        StringBuilder strBuilder = new StringBuilder();

        try {
            Objects.requireNonNull(comparisonResult);

            strBuilder.append("Comparison Result: ").append(comparisonResult.getComparisonResultType()).append("\n");

            if (Objects.isNull(comparisonResult.getDestinationInstance())) {
                strBuilder.append("Destination was not found\n");
            } else {
                strBuilder.append("Destination Keys {");

                // include either the primary keys or the business keys of the destination
                Map<String, Object> destinationKeysAndValues =
                        getDestinationKeysAndValues(comparisonResult.getDestinationInstance());
                destinationKeysAndValues.entrySet().stream().forEach(entrySet ->
                        strBuilder.append(entrySet.getKey()).append(":").append(entrySet.getValue()));

                strBuilder.append("}\n");
            }

            strBuilder.append("Message: ").append(comparisonResult.getMessage().orElse("")).append("\n");

            // include the fields that failed comparison in a tabular format
            strBuilder.append(TableBuilder.buildTable(comparisonResult));
        } catch (Exception e) {
            LOG.error("Unable to build complete message", e);
            strBuilder.append("Unable to build complete message, please review the logs for details");
        }

        return strBuilder.toString();
    }

    private Map<String, Object> getDestinationKeysAndValues(ModelInstance destinationInstance) {
        Map<String, Object> keysAndValues = Collections.emptyMap();

        // first we will look for primary key column names. if those are present then we will use them. if they are
        // not present then we will use the business keys if they are present
        Set<String> keyColumnNames =
                Utils.getDestinationPrimaryKeyColumnNames(destinationInstance.getModelDefinition());

        if (CollectionUtils.isNotEmpty(keyColumnNames)) {
            keysAndValues = keyColumnNames.stream()
                    .map(keyColumnName -> new ImmutablePair<>(keyColumnName, destinationInstance.get(keyColumnName)))
                    .filter(pair -> Objects.nonNull(pair.getValue()))
                    .collect(Collectors.toMap(ImmutablePair::getKey, ImmutablePair::getValue));
        }

        if (keysAndValues.isEmpty()
                && tableColumnService.definitionContainsBKColumns(destinationInstance.getModelDefinition())) {
            keysAndValues = tableColumnService
                    .sortAndExtractNonHousekeepingColumns(destinationInstance.getModelDefinition(),
                            TableColumnService.BK_COLUMNS_PREDICATE)
                    .stream()
                    .map(tableColumnType -> new ImmutablePair<>(tableColumnType.getName(),
                            destinationInstance.get(tableColumnType.getName())))
                    .filter(pair -> Objects.nonNull(pair.getValue()))
                    .collect(Collectors.toMap(ImmutablePair::getKey, ImmutablePair::getValue));
        }

        return keysAndValues;
    }

    private static class TableBuilder {
        static final String HDR_NAME = "Column Name";
        static final String HDR_DATA_TYPE = "Data Type";
        static final String HDR_COMPARED_VALUE = "Compared Value";
        static final String HDR_DESTINATION_VALUE = "Destination Value";

        static final int HDR_NAME_IDX = 0;
        static final int HDR_DATA_TYPE_IDX = 1;
        static final int HDR_COMPARED_VALUE_IDX = 2;
        static final int HDR_DESTINATION_VALUE_IDX = 3;

        static String buildTable(ComparisonResult comparisonResult) {
            StringBuilder strBuilder = new StringBuilder();

            // only build the table if we have fields that have failed comparison
            if (CollectionUtils.isNotEmpty(comparisonResult.getFieldComparisonFailures())) {
                // first get the columns
                List<Column> columns = getColumns(comparisonResult);

                addHeaders(columns, strBuilder);
                addDataRows(columns, strBuilder);
            }

            return strBuilder.toString();
        }

        private static List<Column> getColumns(ComparisonResult comparisonResult) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

            // pre-define each Column with size defaulting to length of the header text
            List<Column> columns = new ArrayList<>();
            columns.add(new Column(HDR_NAME, HDR_NAME.length()));
            columns.add(new Column(HDR_DATA_TYPE, HDR_DATA_TYPE.length()));
            columns.add(new Column(HDR_COMPARED_VALUE, HDR_COMPARED_VALUE.length()));
            columns.add(new Column(HDR_DESTINATION_VALUE, HDR_DESTINATION_VALUE.length()));

            // loop through each FieldComparisonFailure doing the following for each column:
            // 1. ensuring the size of the column is as wide as the largest data element.
            // 2. adding each data element
            comparisonResult.getFieldComparisonFailures().stream().forEach(fieldComparisonFailure -> {
                Column nameColumn = columns.get(HDR_NAME_IDX);
                nameColumn.size =
                        Math.max(nameColumn.size, fieldComparisonFailure.getTableColumnType().getName().length());
                nameColumn.data.add(fieldComparisonFailure.getTableColumnType().getName());

                Column dataTypeColumn = columns.get(HDR_DATA_TYPE_IDX);
                dataTypeColumn.size = Math.max(dataTypeColumn.size,
                        fieldComparisonFailure.getTableColumnType().getType().name().length());
                dataTypeColumn.data.add(fieldComparisonFailure.getTableColumnType().getType().name());

                addToColumn(columns.get(HDR_COMPARED_VALUE_IDX), fieldComparisonFailure.getCompareToValue(), sdf);
                addToColumn(columns.get(HDR_DESTINATION_VALUE_IDX), fieldComparisonFailure.getDestinationValue(), sdf);
            });

            return columns;
        }

        private static void addToColumn(Column column, Optional<Object> optionalValue, SimpleDateFormat sdf) {
            String valueStr = "";

            if (optionalValue.isPresent()) {
                Object value = optionalValue.get();

                // if the value is a java.util.Date or java.time.LocalDateTime format it using the appropriate
                // formatter. otherwise just convert it to a string.
                if (Date.class.isInstance(value)) {
                    valueStr = sdf.format(value);
                } else if (LocalDateTime.class.isInstance(value)) {
                    valueStr = DATE_TIME_FORMATTER.format((LocalDateTime) value);
                } else {
                    valueStr = value.toString();
                }
            }

            column.size = Math.max(column.size, valueStr.length());
            column.data.add(valueStr);
        }

        private static void addHeaders(List<Column> columns, StringBuilder strBuilder) {
            // add the name of the column padding it so that it is a fixed width based on the size of the column
            columns.stream().forEach(column -> {
                strBuilder.append(StringUtils.rightPad(column.name.toUpperCase(), column.size));

                // add a space between each column
                strBuilder.append(" ");
            });
            strBuilder.append("\n");

            // add a separator to each column
            columns.stream().forEach(column -> {
                strBuilder.append(StringUtils.repeat("-", column.size));
                strBuilder.append(" ");
            });
            strBuilder.append("\n");
        }

        private static void addDataRows(List<Column> columns, StringBuilder strBuilder) {
            // use one column to get the number of rows
            int numberOfRows = columns.get(0).data.size();

            // loop which will define the current index/row number. and then get the data from each column using the
            // index. when adding the data pad spaces so that each column is a fixed based on the size of the column.
            for (int i = 0; i < numberOfRows; ++i) {
                final int rowNumber = i;
                columns.stream().forEach(column -> {
                    strBuilder.append(StringUtils.rightPad(column.data.get(rowNumber), column.size));
                    strBuilder.append(" ");
                });
                strBuilder.append("\n");
            }
        }
    }

    private static class Column {
        private String name;
        private int size;
        private List<String> data;

        Column(String name, int size) {
            this.name = name;
            this.size = size;
            this.data = new ArrayList<>();
        }
    }
}
