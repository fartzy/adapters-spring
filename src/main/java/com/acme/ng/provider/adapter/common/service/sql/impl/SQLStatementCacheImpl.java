package com.acme.ng.provider.adapter.common.service.sql.impl;

import com.acme.ng.provider.adapter.common.model.DatabaseStatementType;
import com.acme.ng.provider.adapter.common.model.SQLDialect;
import com.acme.ng.provider.adapter.common.service.sql.SQLStatementCache;
import com.acme.ng.provider.model.common.ModelDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author
 * @date 4/8/18
 */
@Service
public class SQLStatementCacheImpl implements SQLStatementCache {

    private final SQLStatementGeneratorServiceFactory sqlGeneratorServiceFactory;


    private final String schemaName;

    private final SQLDialect dialect;

    public SQLStatementCacheImpl(SQLStatementGeneratorServiceFactory sqlGeneratorServiceFactory,
                                 @Value("${destination.repository.rdbms.dbschema:#{null}}") String schemaName,
                                 SQLDialect dialect) {
        this.sqlGeneratorServiceFactory = sqlGeneratorServiceFactory;
        this.schemaName = schemaName;
        this.dialect = dialect;
    }

    @Override
    public String getSQLStatement(ModelDefinition modelDefinition, DatabaseStatementType type) {
        if (modelDefinition.getDefInstanceID() == null) {
            modelDefinition.setDefInstanceID(modelDefinition.getDefID());
        }

        final Map<DatabaseStatementType, String> statements = sqlGeneratorServiceFactory
                .getByDialect(dialect)
                .generateSQLStatements(modelDefinition, schemaName);

        return statements.get(type);
    }
}
