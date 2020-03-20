package com.acme.ng.provider.adapter.rest.extension.service.model;

public class Sql {

    public final static String INFORMATION_SCHEMA_QUERY =
            "SELECT KCU1.CONSTRAINT_NAME AS FK_CONSTRAINT_NAME " +
                    ",KCU1.TABLE_NAME AS FK_TABLE_NAME " +
                    ",KCU1.COLUMN_NAME AS FK_COLUMN_NAME " +
                    ",KCU1.ORDINAL_POSITION AS FK_ORDINAL_POSITION" +
                    ",KCU2.CONSTRAINT_NAME AS REFERENCED_CONSTRAINT_NAME" +
                    ",KCU2.TABLE_NAME AS REFERENCED_TABLE_NAME" +
                    ",KCU2.COLUMN_NAME AS REFERENCED_COLUMN_NAME" +
                    ",KCU2.ORDINAL_POSITION AS REFERENCED_ORDINAL_POSITION " +
                    "FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS AS RC " +

                    "INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE AS KCU1 " +
                    "ON KCU1.CONSTRAINT_CATALOG = RC.CONSTRAINT_CATALOG " +
                    "AND KCU1.CONSTRAINT_SCHEMA = RC.CONSTRAINT_SCHEMA " +
                    "AND KCU1.CONSTRAINT_NAME = RC.CONSTRAINT_NAME " +

                    "INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE AS KCU2 " +
                    "ON KCU2.CONSTRAINT_CATALOG = RC.UNIQUE_CONSTRAINT_CATALOG " +
                    "AND KCU2.CONSTRAINT_SCHEMA = RC.UNIQUE_CONSTRAINT_SCHEMA " +
                    "AND KCU2.CONSTRAINT_NAME = RC.UNIQUE_CONSTRAINT_NAME " +
                    "AND KCU2.ORDINAL_POSITION = KCU1.ORDINAL_POSITION ";

    public final static String SELECT = "SELECT * FROM ";
    public final static String DOT = ".";
    public final static String FK_TABLE_NAME = "fk_table_name";
    public final static String REF_TABLE_NAME = "referenced_table_name";
    public final static String CONSTRAINT_NAME = "fk_constraint_name";
    public final static String FK_COL_NAME = "fk_column_name";
    public final static String REF_COL_NAME = "referenced_column_name";

}
