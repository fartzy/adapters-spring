package com.acme.ng.provider.adapter.rest.extension.service.service;

import com.acme.ng.provider.adapter.rest.extension.service.model.Constraint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.acme.ng.provider.adapter.rest.extension.service.model.Sql;
import static com.acme.ng.provider.adapter.rest.extension.service.model.Sql.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;



@Service
public class NestedTableWriter2 {
    private static final Logger LOG = LoggerFactory.getLogger(NestedTableWriter2.class);

    @Autowired
    @Qualifier("ptdm_sim")
    private JdbcTemplate jdbcTemplate;

    @Value("#{'${tables.inner}'.split(',')}")
    private List<String> tables;

    @Value("${adapter.performInstanceComparison}") //set to false to skip compare logic. default is true
    private boolean performInstanceComparison;

    @Value("#{'${tables.outer}'.toUpperCase()}")
    private String outerTable;

    @Value("#{'${destination.repository.rdbms.dbschema}'.toUpperCase()}")
    private String schemaName;

    private Map<String,List<Map<String,Object>>> mapOfTableDatas;
    private List<Constraint> constraintList = new ArrayList<Constraint>();
    private Map<String, List<String>> constraintMap = new HashMap<String, List<String>>();

    private final ObjectMapper mapper = new ObjectMapper();

    public void run() {

        mapOfTableDatas = new HashMap<String,List<Map<String,Object>>>();
        mapOfTableDatas.put(outerTable, jdbcTemplate.queryForList(SELECT + schemaName + DOT + outerTable));

        populateGlobalConstraintData(jdbcTemplate
                .queryForList(Sql.INFORMATION_SCHEMA_QUERY)
                .stream()
                .filter(constraintRow -> constraintRow
                        .get(FK_TABLE_NAME)
                        .toString()
                        .equalsIgnoreCase(outerTable))
                .collect(Collectors.toList()));

        populateGlobalConstraintData(jdbcTemplate
                .queryForList(Sql.INFORMATION_SCHEMA_QUERY)
                .stream()
                .filter(constraintRow -> !(constraintRow
                        .get(FK_TABLE_NAME)
                        .toString()
                        .equalsIgnoreCase(outerTable)))
                .collect(Collectors.toList()));

        try {
            ObjectNode objNode = mapper.createObjectNode();
            for (Map rowMap : mapOfTableDatas.get(outerTable)) {

                objNode.put(outerTable, mapper.writeValueAsString(recurseThroughData(constraintMap, outerTable, rowMap)));
                System.out.println(mapper.writeValueAsString(objNode).replace("\\",""));
                objNode = mapper.createObjectNode();

            }
        }
        catch (IOException ex) {
            LOG.error("Could not write results... " + ex.getStackTrace());
        }// end catch


    }// end run



    public Map <String, Object>  recurseThroughData(Map<String, List<String>> cMap, String root, Map<String, Object> rowMap){
        if (cMap.size() == 0) return rowMap;
        List<String> tables = cMap.get(root.toUpperCase());
        String temp;
        try {

            temp = mapper.writeValueAsString(rowMap);
            for (String referencedTable : tables) {

                Map columnMap = Constraint.getConstraintColumnMapByRefTable(referencedTable, root, constraintList);
                String fKey1, refKey1, fKey2, refKey2, fKeyColName1, refKeyColName1, fKeyColName2, refKeyColName2;
                if (columnMap.size() == 1) {

                    Map.Entry<String, String> entry = (Map.Entry<String, String>)columnMap.entrySet().stream().findFirst().get();
                    fKeyColName1 = entry.getKey();
                    refKeyColName1 = entry.getValue();

                    for (Map refRowMap : mapOfTableDatas.get(referencedTable)) {
                        fKey1 = rowMap.get(fKeyColName1).toString().toUpperCase();
                        refKey1 = refRowMap.get(refKeyColName1).toString().toUpperCase();

                        if (fKey1.equals(refKey1)) {

                            //get rid of the root before recursing
                            Map tmp = new HashMap(cMap);
                            tmp.remove(root);
                            recurseThroughData(tmp, referencedTable, (Map<String, Object>)rowMap.put(referencedTable, mapper.writeValueAsString(refRowMap)));
                        }
                    }
                }
                else if (columnMap.size() == 2) {

                    Map.Entry<String, String> entry = (Map.Entry<String, String>)columnMap.entrySet().stream().findFirst().get();
                    fKeyColName1 = entry.getKey();
                    refKeyColName1 = entry.getValue();

                    Map.Entry<String, String> entry1 = (Map.Entry<String, String>)columnMap.entrySet().stream().skip(1).findFirst().get();
                    fKeyColName2 = entry1.getKey();
                    refKeyColName2 = entry1.getValue();

                    for (Map refRowMap : mapOfTableDatas.get(referencedTable)) {
                        fKey1 = rowMap.get(fKeyColName1).toString().toUpperCase();
                        refKey1 = refRowMap.get(refKeyColName1).toString().toUpperCase();
                        fKey2 = rowMap.get(fKeyColName2).toString().toUpperCase();
                        refKey2 = refRowMap.get(refKeyColName2).toString().toUpperCase();

                        if (fKey1.equals(refKey1) && fKey2.equals(refKey2)) {

                            //get rid of the root before recursing
                            Map tmp = new HashMap(cMap);
                            tmp.remove(root);
                            recurseThroughData(tmp, referencedTable, (Map<String, Object>)rowMap.put(referencedTable, mapper.writeValueAsString(refRowMap)));
                        }// end if
                    }// end for
                }// end else if
            }// end for
        }// end try

        catch (IOException ex) {
            LOG.error("Could not write results... " + ex.getStackTrace());
        }

        return rowMap;
    }


    private void populateGlobalConstraintData(List<Map<String,Object>> informationSchemaResultSet) {
        informationSchemaResultSet.forEach(conStrRow -> {
                    final String refTableName = conStrRow.get(REF_TABLE_NAME).toString().toUpperCase();
                    final String constraintName = conStrRow.get(CONSTRAINT_NAME).toString().toUpperCase();
                    final String refColName = conStrRow.get(REF_COL_NAME).toString().toUpperCase();
                    final String fkColName = conStrRow.get(FK_COL_NAME).toString().toUpperCase();
                    final String fkTableName = conStrRow.get(FK_TABLE_NAME).toString().toUpperCase();

                    mapOfTableDatas.putIfAbsent(refTableName,jdbcTemplate.queryForList(SELECT + schemaName + DOT + refTableName));

                    //add the Costraint Object RefTable to the Constraint List
                    if (Constraint.containsByConstraintName(constraintList,constraintName)){
                        Constraint.lookupByConstraintName(constraintList, constraintName)
                                .addToConstraintColumnMap(fkColName, refColName);
                    }

                    else { constraintList.add(new Constraint(constraintName, fkTableName, fkColName, refTableName, refColName)); }

                    //add FKCol, RefCol to Constraint Map to the constraint Map for the Constraint Object of the `constraintName` constraint
                    if (constraintMap.containsKey(fkTableName) && !(constraintMap.get(fkTableName).contains(refTableName))){
                        constraintMap.put(fkTableName, addToList(constraintMap.get(fkTableName), refTableName));
                    }
                    else if (!(constraintMap.containsKey(fkTableName))) {
                        constraintMap.put(fkTableName, addToList(new ArrayList<String>(), refTableName));
                    }
                }
        );
    }

    //Helper method to return a list after adding an element
    private List<String> addToList (List<String> list, String element) {
        list.add(element);
        return list;
    }

}

