package com.acme.ng.provider.adapter.common.service.sql.impl;

import com.acme.ng.provider.adapter.common.model.SQLDialect;
import com.acme.ng.provider.adapter.common.service.sql.SQLStatementGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author 
 * @date 4/15/18
 */
@Component
public class SQLStatementGeneratorServiceFactory {

    @Autowired
    @Qualifier("ansi")
    private SQLStatementGeneratorService ansiSQLGeneratorService;

    @Autowired
    @Qualifier("postgres")
    private SQLStatementGeneratorService postgresSQLStatementGeneratorService;

   // @Autowired
    //@Qualifier("oracle")
    //private SQLStatementGeneratorService oracleSQLStatementGeneratorService;

    //@Autowired
    //@Qualifier("db2")
   // private SQLStatementGeneratorService db2SQLStatementGeneratorService;

    public SQLStatementGeneratorService getByDialect(SQLDialect dialect) {
        switch (dialect) {
            case ANSI:
                return ansiSQLGeneratorService;
            case POSTGRES:
                return postgresSQLStatementGeneratorService;
   //         case ORACLE:
  //              return oracleSQLStatementGeneratorService;
  //          case DB2:
  //              return db2SQLStatementGeneratorService;
            default:
                throw new IllegalArgumentException("Dialect is not supported: " + dialect);
        }
    }
}
