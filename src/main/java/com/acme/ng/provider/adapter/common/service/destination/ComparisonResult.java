package com.acme.ng.provider.adapter.common.service.destination;

import com.acme.ng.provider.model.ModelInstance;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * This immutable class provides the results of a comparison against destination data.
 * Created by Julian M on 10/3/18.
 */
public class ComparisonResult {
    public enum ComparisonResultType {NOT_EXECUTED_ACCEPTABLE, NOT_EXECUTED_UNACCEPTABLE, FAILURE, SUCCESS}

    protected ComparisonResultType comparisonResultType;
    protected String message;
    protected List<FieldComparisonFailure> fieldComparisonFailures;
    protected ModelInstance compareToInstance;
    protected ModelInstance destinationInstance;
    protected boolean comparedAgainstPersistingInstance;

    public ModelInstance getCompareToInstance() {
        return compareToInstance;
    }

    public ModelInstance getDestinationInstance() {
        return destinationInstance;
    }

    public List<FieldComparisonFailure> getFieldComparisonFailures() {
        return Collections.unmodifiableList(fieldComparisonFailures);
    }

    public ComparisonResultType getComparisonResultType() {
        return comparisonResultType;
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public boolean isFailure() {
        return ComparisonResultType.FAILURE == comparisonResultType;
    }

    public boolean isSuccess() {
        return ComparisonResultType.SUCCESS == comparisonResultType;
    }

    public boolean isComparedAgainstPersistingInstance() {
        return comparedAgainstPersistingInstance;
    }

    public boolean isNotExecutedUnacceptable() {
        return ComparisonResultType.NOT_EXECUTED_UNACCEPTABLE == comparisonResultType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ComparisonResult that = (ComparisonResult) o;
        return Objects.equals(comparisonResultType, that.comparisonResultType)
                && Objects.equals(message, that.message)
                && Objects.equals(fieldComparisonFailures, that.fieldComparisonFailures)
                && Objects.equals(compareToInstance, that.compareToInstance)
                && Objects.equals(destinationInstance, that.destinationInstance)
                && Objects.equals(comparedAgainstPersistingInstance, that.comparedAgainstPersistingInstance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comparisonResultType, message, fieldComparisonFailures, compareToInstance,
                destinationInstance, comparedAgainstPersistingInstance);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("comparisonResultType", comparisonResultType)
                .append("message", message)
                .append("compareToInstance", compareToInstance)
                .append("destinationInstance", destinationInstance)
                .append("fieldComparisonFailures", fieldComparisonFailures)
                .toString();
    }
}
