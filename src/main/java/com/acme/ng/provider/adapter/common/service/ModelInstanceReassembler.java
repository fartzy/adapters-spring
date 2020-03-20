package com.acme.ng.provider.adapter.common.service;

import com.acme.ng.provider.model.ModelInstance;

/**
 * This purpose of this interface to expose the ability to "reassemble" a ModelInstance when an AdapterEvent has been
 * retrieved from the DB. This will ensure the ModelDefinition has been loaded and that and data type conversions have been handled.
 * Created by Julian Montgomery on 10/8/18.
 */
public interface ModelInstanceReassembler {
    /**
     * Reassembles the provided ModelInstance
     *
     * @param modelInstance The ModelInstance to be reassembled
     * @return The reassembled ModelInstance
     */
    ModelInstance reassembleModelInstance(ModelInstance modelInstance);
}
