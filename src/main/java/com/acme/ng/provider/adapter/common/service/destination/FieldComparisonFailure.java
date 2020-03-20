package com.acme.ng.provider.adapter.common.service.destination;

import com.acme.ng.provider.definition.TableColumnType;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;
import java.util.Optional;

/**
 * This class encapsulates the data needed when a field fails comparison
 * Created by Julian M on 10/3/18.
 */
public class FieldComparisonFailure {
    private TableColumnType tableColumnType;
    private Object destinationValue;
    private Object compareToValue;

    public FieldComparisonFailure(TableColumnType tableColumnType, Object destinationValue, Object compareToValue) {
        Objects.requireNonNull(tableColumnType, "unable to create a field comparison failure without a TableColumnType");

        this.tableColumnType = tableColumnType;
        this.destinationValue = destinationValue;
        this.compareToValue = compareToValue;
    }

    public TableColumnType getTableColumnType() {
        return tableColumnType;
    }

    public Optional<Object> getDestinationValue() {
        return Optional.ofNullable(destinationValue);
    }

    public Optional<Object> getCompareToValue() {
        return Optional.ofNullable(compareToValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FieldComparisonFailure that = (FieldComparisonFailure) o;
        return Objects.equals(tableColumnType, that.tableColumnType)
                && Objects.equals(destinationValue, that.destinationValue)
                && Objects.equals(compareToValue, that.compareToValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableColumnType, destinationValue, compareToValue);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("tableColumnType", tableColumnType)
                .append("destinationValue", destinationValue)
                .append("compareToValue", compareToValue)
                .toString();
    }
}
