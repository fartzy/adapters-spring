package com.acme.ng.provider.adapter.common.service.sql;

import com.acme.ng.provider.adapter.common.model.DatabaseStatementType;
import com.acme.ng.provider.model.common.ModelDefinition;

/**
 * @author
 * @date 4/8/18
 */
public interface SQLStatementCache {

    String getSQLStatement(ModelDefinition modelDefinition, DatabaseStatementType type);

}
