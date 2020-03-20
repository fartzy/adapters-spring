package com.acme.ng.provider.adapter.common.service.destination;

import com.acme.ng.provider.model.ModelInstance;

/**
 * @author Mike A
 * @date 4/16/18
 */
public interface DestinationSystemService {


    ModelInstance getDestinationEntityByPrimaryKeys(ModelInstance entity);

    ModelInstance getDestinationEntityByBusinessKeys(ModelInstance entity);

    void updateDestinationEntityByPrimaryKeys(ModelInstance entity);

    void saveDestinationEntity(ModelInstance entity);

}
