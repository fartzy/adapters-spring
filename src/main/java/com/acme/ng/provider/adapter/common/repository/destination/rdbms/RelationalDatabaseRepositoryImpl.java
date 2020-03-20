package com.acme.ng.provider.adapter.common.repository.destination.rdbms;

import com.acme.ng.provider.adapter.common.model.DatabaseStatementType;
import com.acme.ng.provider.adapter.common.repository.destination.DestinationSystemRepository;
import com.acme.ng.provider.adapter.common.service.destination.impl.DestinationSystemServiceImpl;
import com.acme.ng.provider.adapter.common.service.sql.SQLStatementCache;
import com.acme.ng.provider.model.common.ModelDefinition;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.ArrayUtils.toArray;

@Repository
public class RelationalDatabaseRepositoryImpl implements DestinationSystemRepository {

    @Autowired
    private SQLStatementCache sqlStatementCache;

    @Autowired
    @Qualifier("ptdm_sim")
    private JdbcTemplate jdbcTemplate;

    private static final Logger LOG = LoggerFactory.getLogger(RelationalDatabaseRepositoryImpl.class);

    @Override
    public Map<String, Object> selectDestinationEntityByPrimaryKeys(ModelDefinition definition, List<Object> primaryKeysValues) {
        String selectStatement = sqlStatementCache.getSQLStatement(definition, DatabaseStatementType.SELECT_BY_PK);
        List<Map<String, Object>> query = jdbcTemplate.queryForList(selectStatement, primaryKeysValues.toArray());

        return query.isEmpty() ? Collections.emptyMap() : query.get(0);
    }

    @Override
    public Map<String, Object> selectDestinationEntityByBusinessKeys(ModelDefinition definition, List<Object> businessKeysValues) {
        String selectStatement = sqlStatementCache.getSQLStatement(definition, DatabaseStatementType.SELECT_BY_BK);
        if (businessKeysValues.contains(null)) {
            selectStatement = replaceNullValueParametersInSqlStatementWithIsNull(selectStatement, businessKeysValues);
            businessKeysValues.removeIf(Objects::isNull);
        }

        List<Map<String, Object>> query = jdbcTemplate.queryForList(selectStatement, businessKeysValues.toArray());
        return query.isEmpty() ? Collections.emptyMap() : query.get(0);
    }

    @Override
    public void updateDestinationEntityByPrimaryKeys(ModelDefinition definition, List<Object> primaryKeysValues, List<Object> regularColumnsValues) {
        String updateStatement = sqlStatementCache.getSQLStatement(definition, DatabaseStatementType.UPDATE_BY_PK);
        LOG.debug("SQL Statement {}", updateStatement);

        LOG.debug("Values List {}",ListUtils.union(regularColumnsValues, primaryKeysValues));
        jdbcTemplate.update(updateStatement, ListUtils.union(regularColumnsValues, primaryKeysValues).toArray());
    }

    @Override
    public void insertDestinationEntity(ModelDefinition definition, List<Object> allColumnsValues) {
        String insertStatement = sqlStatementCache.getSQLStatement(definition, DatabaseStatementType.INSERT);
        jdbcTemplate.update(insertStatement, allColumnsValues.toArray());
    }

    private String replaceNullValueParametersInSqlStatementWithIsNull(String selectStatement, List<Object> businessKeysValues) {
        StringBuffer sb = new StringBuffer();
        Pattern p = Pattern.compile("= \\?");
        Matcher m = p.matcher(selectStatement);
        int count = 0;
        while(m.find()) {
            count++;
            if (businessKeysValues.get(count-1) == null) {
                m.appendReplacement(sb, "IS NULL");
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
