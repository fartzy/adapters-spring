package com.acme.ng.provider.adapter.common.service.destination.impl;

import com.acme.ng.provider.adapter.common.service.destination.ComparisonResult;
import com.acme.ng.provider.adapter.common.service.destination.FieldComparisonFailure;
import com.acme.ng.provider.model.ModelInstance;

import java.util.Collections;
import java.util.List;

/**
 * This is a package-level ComparisonResult class that provides mutable methods. This is so that
 * DestinationComparisonImpl can set properties on the ComparisonResult but classes outside of this package can only
 * read the properties.
 * Created by Julian M on 10/4/18.
 */
class MutableComparisonResult extends ComparisonResult {
    MutableComparisonResult(ModelInstance compareToInstance, ModelInstance destinationInstance) {
        this.compareToInstance = compareToInstance;
        this.destinationInstance = destinationInstance;
        this.fieldComparisonFailures = Collections.emptyList();
    }

    MutableComparisonResult(ModelInstance compareToInstance, ModelInstance destinationInstance,
                            ComparisonResultType comparisonResultType) {
        this(compareToInstance, destinationInstance);
        this.comparisonResultType = comparisonResultType;
    }

    MutableComparisonResult(ModelInstance compareToInstance, ModelInstance destinationInstance,
                            List<FieldComparisonFailure> fieldComparisonFailures) {
        this(compareToInstance, destinationInstance);
        this.fieldComparisonFailures = fieldComparisonFailures;
    }

    MutableComparisonResult setComparisonResultType(ComparisonResultType comparisonResultType) {
        this.comparisonResultType = comparisonResultType;
        return this;
    }

    MutableComparisonResult add(FieldComparisonFailure fieldComparisonFailure) {
        this.fieldComparisonFailures.add(fieldComparisonFailure);
        return this;
    }

    MutableComparisonResult setMessage(String message) {
        this.message = message;
        return this;
    }

    MutableComparisonResult setComparedAgainstPersistingInstance(boolean comparedAgainstPersistingInstance) {
        this.comparedAgainstPersistingInstance = comparedAgainstPersistingInstance;
        return this;
    }
}
