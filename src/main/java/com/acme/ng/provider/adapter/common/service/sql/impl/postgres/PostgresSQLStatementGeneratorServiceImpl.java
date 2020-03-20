package com.acme.ng.provider.adapter.common.service.sql.impl.postgres;

import com.acme.ng.provider.adapter.common.service.sql.impl.ansi.AnsiSQLStatementGeneratorServiceImpl;
import com.healthmarketscience.sqlbuilder.Expression;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Created  4/27/2018.
 */
@Service
@Qualifier("postgres")
public class PostgresSQLStatementGeneratorServiceImpl extends AnsiSQLStatementGeneratorServiceImpl {

    protected Expression getSequenceGeneratedValue(String sequenceName) {
        return new PostgresSequenceGeneratedValue(sequenceName);
    }
}


