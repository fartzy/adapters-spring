package com.acme.ng.provider.adapter.common.service.destination;

import com.acme.ng.provider.model.ModelInstance;

/**
 * This interface defines the ability to compare data against destination data
 * Created by Julian M on 10/3/18.
 */
public interface DestinationComparison {
    /**
     * Compares either the previous or persisting instance data against the destination. If the previousInstance is not
     * null it will be used, otherwise the persistingInstance will be used. The persistingInstance is required.
     *
     * @param destinationInstance The destination data
     * @param persistingInstance  The data to be persisted to the destination
     * @param previousInstance    The previous snapshot of destination data
     * @return ComparisonResult
     */
    ComparisonResult compareToDestination(ModelInstance destinationInstance, ModelInstance persistingInstance,
                                          ModelInstance previousInstance);
}
