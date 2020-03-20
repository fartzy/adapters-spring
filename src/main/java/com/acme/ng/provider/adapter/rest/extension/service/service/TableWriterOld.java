package com.acme.ng.provider.adapter.rest.extension.service.service;

import com.acme.ng.provider.adapter.rest.extension.service.model.Constraint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;


import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class TableWriterOld {
    private static final Logger LOG = LoggerFactory.getLogger(TableWriterOld.class);

    @Autowired
    @Qualifier("ptdm_sim")
    private JdbcTemplate jdbcTemplate;

    @Value("#{'${tables.inner}'.split(',')}")
    private List<String> tables;

    @Value("#{'${tables.outer}'.toUpperCase()}")
    private String outerTable;

    @Value("#{'${destination.repository.rdbms.dbschema}'.toUpperCase()}")
    private String schemaName;

    Map<String,List<Map<String,Object>>> mapOfTableDatas = new HashMap<String,List<Map<String,Object>>>();
    List<Constraint> çonstraintList = new ArrayList<Constraint>();
    Map<String, List<String>> constraintMap = new HashMap<String, List<String>>();

    private final ObjectMapper mapper = new ObjectMapper();

    String informationSchemaQuery =
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


    public void run() {

        mapOfTableDatas.put(outerTable, jdbcTemplate.queryForList("SELECT * FROM " + schemaName + "." + outerTable));
        mapOfTableDatas.put(tables.get(0), jdbcTemplate.queryForList("SELECT * FROM " + schemaName + "." + tables.get(0)));
        if (tables.size() == 2) mapOfTableDatas.put(tables.get(1), jdbcTemplate.queryForList("SELECT * FROM " + schemaName + "." + tables.get(1)));


        List<Map<String,Object>> is1 = jdbcTemplate.queryForList(informationSchemaQuery);


        String surname = (String) jdbcTemplate.queryForObject("select name from rest_test.M_2_KEYS_CONSUMER where key1 = ?", new Object[]{new Long(1)}, String.class);

        List<Map<String,Object>> outerConstraints = is1.stream()
            .filter(constraintRow -> constraintRow.get("fk_table_name").toString().equalsIgnoreCase(outerTable)).collect(Collectors.toList());

        List<Map<String,Object>> innerConstraints = is1.stream()
            .filter(constraintRow -> !(constraintRow.get("fk_table_name").toString().equalsIgnoreCase(outerTable))).collect(Collectors.toList());




        populateConstraintData(outerConstraints);
        populateConstraintData(innerConstraints);

        String tbl1 = tables.get(0);
        String tbl2 = "";
        if (tables.size() == 2) tbl2 = tables.get(1);

        //TODO: The below statment if changed so that it is not erroring out.  But passing a null will never work as that argument, it is not a valid refTable
        Map cMapOuter = Constraint.lookupByFKTableAndRefTable(çonstraintList, outerTable, null).getConstraintColumnMap();
        Map.Entry<String, String> entry = ( Map.Entry<String, String>)cMapOuter.entrySet().stream().findFirst().get();
        String firstOuterKey = entry.getKey();
        String firstOuterValue = entry.getValue();
        String key1 = "";
        String val1 = "";
        if (tables.size() == 2) {
            Map cMap1 = Constraint.lookupByFKTableAndRefTable(çonstraintList, tbl1, null).getConstraintColumnMap();
            Map.Entry<String, String> entry1 = (Map.Entry<String, String>) cMap1.entrySet().stream().findFirst().get();
            key1 = entry1.getKey();
            val1 = entry1.getValue();
        }


        try {
            for (Map rm1 : mapOfTableDatas.get(outerTable)) {
                for (Map rm2 : mapOfTableDatas.get(tbl1)) {
                    if (rm1.get(firstOuterKey).toString().equals(rm2.get(firstOuterValue).toString())) {
                        if (tables.size() == 2) {
                            for (Map rm3 : mapOfTableDatas.get(tbl2)) {
                            if (rm2.get(key1).toString().equals(rm3.get(val1).toString())) {
                                rm2.put(tbl2, mapper.writeValueAsString(rm3));
                                rm1.put(tbl1, mapper.writeValueAsString(rm2));
                                System.out.println(mapper.writeValueAsString(rm1).replace("\\",""));

                            }// end if
                          }// end for
                        }// end if (tables.size() == 2)
                        rm1.put(tbl1, mapper.writeValueAsString(rm2));
                        System.out.println(mapper.writeValueAsString(rm1).replace("\\",""));
                    }
                }
            }
        }
        catch (JsonProcessingException jpe) {
            LOG.error("Could not write results... " + jpe.getStackTrace());
        }
        catch (IOException ioe) {
            LOG.error("Could not create JsonObject... " + ioe.getStackTrace());
        }

       // for (Map rowMap : mapOfTableDatas.get(outerTable)){
       //     Map<String,Object> m = recurseThroughData(constraintMap, rowMap, outerTable);
       //     System.out.println(m);
       //
       //      }
    }



    public Map<String,Object> recurseThroughData(Map<String, List<String>> cMap, Map<String,Object> rowMap, String root){
        if (cMap.size() == 0) return rowMap;
        List<String> tables = cMap.get(root.toUpperCase());
        String temp;
        try {

            temp = mapper.writeValueAsString(rowMap);
            for (String referencedTable : tables) {

                Map columnMap = getConstraintsByReferencedTable(referencedTable, root);
                if (columnMap.size() == 1) {
                    String refKey = (String) columnMap.values().toArray()[0];
                    String fKey = (String) columnMap.get(columnMap.keySet().toArray()[0]);
                    for (Map refRowMap : mapOfTableDatas.get(referencedTable)) {

                        if (rowMap.get(fKey).toString().equalsIgnoreCase(refRowMap.get(refKey).toString())) {
                            rowMap.put(referencedTable, mapper.writeValueAsString(refRowMap));
                            Map tmp = new HashMap(cMap);
                            tmp.remove(root);
                            recurseThroughData(tmp, refRowMap, referencedTable);
                        }
                    }
                }
                else if (columnMap.size() == 2) {
                    Iterator i = columnMap.entrySet().iterator();
                    i.next();
                    Map.Entry<String,String> entry = (Map.Entry<String,String>)i.next();
                    String fKey= entry.getKey();
                    String refKey=entry.getValue();
                    i.next();
                    Map.Entry<String,String> entry1 = (Map.Entry<String,String>)i.next();
                    String fKey1= entry1.getKey();
                    String refKey1=entry1.getValue();

                    for (Map refRowMap : mapOfTableDatas.get(referencedTable)) {

                        if (rowMap.get(fKey).toString().equalsIgnoreCase(refRowMap.get(refKey).toString())
                        && rowMap.get(fKey1).toString().equalsIgnoreCase(refRowMap.get(refKey1).toString())) {
                            rowMap.put(referencedTable, mapper.writeValueAsString(refRowMap));
                            Map tmp = new HashMap(cMap);
                            tmp.remove(root);
                            recurseThroughData(tmp, refRowMap, referencedTable);
                        }
                    }
                }

            }
        }
        catch (JsonProcessingException jpe) {
            LOG.error("Could not write results... " + jpe.getStackTrace());
        }
        catch (IOException ioe) {
            LOG.error("Could not create JsonObject... " + ioe.getStackTrace());
        }
        return rowMap;
    }


    public void populateConstraintData(List<Map<String,Object>> informationSchemaResultSet) {
        informationSchemaResultSet.forEach(conStrRow -> {
                    mapOfTableDatas.putIfAbsent(conStrRow.get("referenced_table_name").toString().toUpperCase(),
                            jdbcTemplate.queryForList("SELECT * FROM " + schemaName + "." + conStrRow.get("referenced_table_name").toString().toUpperCase()));
                    if (çonstraintList.contains(conStrRow.get("fk_constraint_name").toString().toUpperCase())){
                        çonstraintList
                                .stream()
                                .filter(p -> p.equals(conStrRow.get("fk_constraint_name").toString().toUpperCase()))
                                .collect(Collectors.toList())
                                .get(0)
                                .addToConstraintColumnMap(conStrRow.get("fk_column_name").toString().toUpperCase()
                                        , conStrRow.get("referenced_table_name").toString().toUpperCase() );
                    }
                    else {
                        çonstraintList.add(new Constraint(conStrRow.get("fk_constraint_name").toString().toUpperCase(),
                                conStrRow.get("fk_table_name").toString().toUpperCase(),
                                conStrRow.get("fk_column_name").toString().toUpperCase(),
                                conStrRow.get("referenced_table_name").toString().toUpperCase(),
                                conStrRow.get("referenced_column_name").toString().toUpperCase()));
                    }
                    if (constraintMap.containsKey(conStrRow.get("fk_table_name").toString().toUpperCase())){
                        constraintMap.put(conStrRow.get("fk_table_name").toString().toUpperCase(),
                                addToList(constraintMap.get(conStrRow.get("fk_table_name").toString().toUpperCase()), conStrRow.get("referenced_table_name").toString().toUpperCase()));
                    }
                    else if (constraintMap.containsKey(conStrRow.get("fk_table_name").toString().toUpperCase()) &&
                               constraintMap
                                       .get(conStrRow.get("fk_table_name").toString().toUpperCase())
                                       .contains(conStrRow.get("referenced_table_name").toString())) {

                    }
                    //else the value is not there but the table already has some values

                    else {
                        constraintMap.put(conStrRow.get("fk_table_name").toString().toUpperCase(), addToList(new ArrayList<String>(), conStrRow.get("referenced_table_name").toString().toUpperCase()));
                    }
                }
        );
    }

    private List<String> addToList (List<String> list, String element) {
        list.add(element);
        return list;
    }

    private Map getConstraintsByReferencedTable (String referencedTable, String fkTable) {
        for (Constraint c: çonstraintList) {
            if (c.getFkTableName().equalsIgnoreCase(fkTable) && c.getReferencedTableName().equalsIgnoreCase(referencedTable)){
                return c.getConstraintColumnMap();
            }
        }
        return new HashMap<String, String>();
    }
}

