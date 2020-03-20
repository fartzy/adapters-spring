package com.acme.ng.provider.adapter.common.domain;

import com.acme.ng.provider.model.ModelInstance;

import java.util.Map; /**
 * 
 */
public class SyncResult {
    private final ModelInstance modelInstance;
    private final Map<String, Object> changedFields;

    public SyncResult(ModelInstance modelInstance, Map<String, Object> changedFields) {
        this.modelInstance = modelInstance;
        this.changedFields = changedFields;
    }

    public ModelInstance getModelInstance() {
        return modelInstance;
    }

    public Map<String, Object> getChangedFields() {
        return changedFields;
    }
}
