package com.acme.ng.provider.adapter.common.repository.destination;

import com.acme.ng.provider.model.common.ModelDefinition;

import java.util.List;
import java.util.Map;

/**
 * 
 * @date 4/10/18
 */
public interface DestinationSystemRepository {

    Map<String, Object> selectDestinationEntityByPrimaryKeys(ModelDefinition definition, List<Object> primaryKeysValues);

    Map<String, Object> selectDestinationEntityByBusinessKeys(ModelDefinition definition, List<Object> businessKeysValues);

    void updateDestinationEntityByPrimaryKeys(ModelDefinition definition, List<Object> primaryKeysValues, List<Object> regularColumnsValues);

    void insertDestinationEntity(ModelDefinition definition, List<Object> allColumnsValues);
}
