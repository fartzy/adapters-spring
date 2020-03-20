package com.acme.ng.provider.adapter.rest.extension.service.service;

import com.acme.ng.provider.adapter.rest.extension.service.model.Constraint;
import com.acme.ng.provider.adapter.rest.extension.service.model.TableConfigContainer;
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
public class NestedTableWriter {
    private static final Logger LOG = LoggerFactory.getLogger(NestedTableWriter.class);

    @Autowired
    @Qualifier("ptdm_sim")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TableConfigParser  TblConfigParser;

    @Value("#{'${tables.inner}'.split(',')}")
    private List<String> tables;

    @Value("#{'${tables.outer}'.split(',')}")
    private List<String> outerTables;

    @Value("#{'${destination.repository.rdbms.dbschema}'.toUpperCase()}")
    private String schemaName;

    @Value("${ignoreDatabaseConstraints}")
    private boolean ignoreDatabaseConstraints;

    TableConfigContainer tblConfigCont;

    private Map<String,List<Map<String,Object>>> mapOfTableDatas;
    private List<Constraint> constraintList = new ArrayList<Constraint>();
    private Map<String, List<String>> constraintMap = new HashMap<String, List<String>>();

    private final ObjectMapper mapper = new ObjectMapper();

    public void run() {
        outerTables = outerTables.stream().map(s -> s.toUpperCase()).collect(Collectors.toList());
        tables = tables.stream().map(s -> s.toUpperCase()).collect(Collectors.toList());
        Map<String, List<String>> tempConstraintMap;

        if (ignoreDatabaseConstraints) {

            tblConfigCont = TblConfigParser.parse();
            constraintList = tblConfigCont.getConstraintList();
            tempConstraintMap = tblConfigCont.getConstraintMap();
            mapOfTableDatas = tblConfigCont.getMapOfTableDatas();

            for (Constraint c : constraintList) {
                mapOfTableDatas.putIfAbsent(c.getFkTableName(), jdbcTemplate.queryForList(SELECT + schemaName + DOT + c.getFkTableName()));
                mapOfTableDatas.putIfAbsent(c.getReferencedTableName(), jdbcTemplate.queryForList(SELECT + schemaName + DOT + c.getReferencedTableName()));
            }

            tempConstraintMap.forEach((k, v) -> {
                if (outerTables.contains((String) k)) constraintMap.put(k, v);
            });
            tempConstraintMap.forEach((k, v) -> {
                if (tables.contains((String) k)) constraintMap.putIfAbsent(k, v);
            });

        } else {

            mapOfTableDatas = new HashMap<String,List<Map<String,Object>>>();

            for (String table : tables) {

                mapOfTableDatas.putIfAbsent(table, jdbcTemplate.queryForList(SELECT + schemaName + DOT + table));

                populateGlobalConstraintData(jdbcTemplate
                        .queryForList(Sql.INFORMATION_SCHEMA_QUERY)
                        .stream()
                        .filter(constraintRow -> !(constraintRow
                                .get(FK_TABLE_NAME)
                                .toString()
                                .equalsIgnoreCase(table)))
                        .collect(Collectors.toList()));
            }

            tempConstraintMap = new HashMap<>(constraintMap);

            constraintMap = constraintMap.entrySet()
                    .stream()
                    .filter(x -> outerTables.contains(x.getKey()))
                    .collect(Collectors.toMap(m -> m.getKey(), m -> m.getValue()));


            for (Map.Entry<String, List<String>> entry : tempConstraintMap.entrySet()) {
                constraintMap.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        ObjectNode objNode = mapper.createObjectNode();
        for (String outer : outerTables ) {
            try {

                for (Map rowMap : mapOfTableDatas.get(outer)) {

                    objNode.put(outer, mapper.writeValueAsString(recurseThroughData(constraintMap, outer, rowMap)));
                    System.out.println(mapper.writeValueAsString(objNode).replace("\\", "") + "\n");
                    objNode = mapper.createObjectNode();

                }
            } catch (IOException ex) {
                LOG.error("Could not write results... " + ex.getStackTrace());
            }// end catch


        }// end run

    }

    public Map <String, Object>  recurseThroughData(Map<String, List<String>> cMap, String root, Map<String, Object> rowMap){
        if (cMap.size() == 0) return rowMap;
        List<String> tables = cMap.get(root.toUpperCase());
        String temp;
        try {
            temp = mapper.writeValueAsString(rowMap);
            for (String referencedTable : tables) {

                Map columnMap = Constraint.getConstraintColumnMapByRefTable(referencedTable, root, constraintList);
                for (Map refRowMap : mapOfTableDatas.get(referencedTable)) {
                    List fMap = new ArrayList<>();
                    columnMap.keySet().forEach((k) -> {
                        String keyValue = columnMap.get(k).toString().toUpperCase();
                        String kKey = k.toString().toUpperCase();
                        String fKey = rowMap.get(kKey).toString().toUpperCase();
                        String fValue = refRowMap.get(keyValue).toString().toUpperCase();
                        fMap.add(fKey.equals(fValue));
                    });
                    if(fMap.stream().allMatch(s -> s.equals(true))){

                        Map tmp = new HashMap(cMap);
                        tmp.remove(root);

                        //This iteration could be the second nesting rowRefMap of the outer rowMap, so need to delete the other downstream refRowMaps from the columnMap
                        cMap.forEach((k,v) -> { if (rowMap.containsKey(k)) tmp.remove(k); } );
                        rowMap.put(referencedTable, recurseThroughData(tmp, referencedTable, refRowMap));
                    }
                }
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



