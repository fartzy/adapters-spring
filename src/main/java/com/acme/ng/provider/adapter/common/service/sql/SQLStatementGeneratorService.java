package com.acme.ng.provider.adapter.common.service.sql;

import com.acme.ng.provider.adapter.common.model.DatabaseStatementType;
import com.acme.ng.provider.model.common.ModelDefinition;

import java.util.Map;

/**
 * @author 
 * @date 4/8/18
 */
public interface SQLStatementGeneratorService {

    Map<DatabaseStatementType, String> generateSQLStatements(ModelDefinition modelDefinition, String schemaName);

}
