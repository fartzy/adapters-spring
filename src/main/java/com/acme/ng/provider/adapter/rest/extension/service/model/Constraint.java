package com.acme.ng.provider.adapter.rest.extension.service.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class Constraint {

    private String constraintName;
    private String fkTableName;
    private String referencedTableName;
    private Map<String, String> constraintColumnMap = new HashMap<String, String>();

    public Constraint(String constraintName, String fkTableName, String fkColumnName, String referencedTableName, String referencedColumnName) {
        this.constraintName = constraintName;
        this.fkTableName = fkTableName;
        this.referencedTableName = referencedTableName;
        constraintColumnMap.put(fkColumnName, referencedColumnName);
    }

    public Map<String, String> getConstraintColumnMap() {
        return constraintColumnMap;
    }

    public void addToConstraintColumnMap(String foreignKeyColumnName, String referencedColumnName) {
        constraintColumnMap.put(foreignKeyColumnName, referencedColumnName);
    }

    public String getConstraintName() {
        return constraintName;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }
    public String getFkTableName() {
        return fkTableName;
    }

    public void setFkTableName(String fkTableName) {
        this.fkTableName = fkTableName;
    }

    public String getReferencedTableName() {
        return referencedTableName;
    }

    public void setReferencedTableName(String referencedTableName) {
        this.referencedTableName = referencedTableName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Constraint that = (Constraint) o;
        return Objects.equals(constraintName, that.constraintName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constraintName);
    }

    public static Constraint lookupByFKTableAndRefTable(List<Constraint> constraints, String fkTable, String refTable) {
        Constraint cTemp = null;
        for (Constraint c : constraints ) {
            if (c.getFkTableName().equalsIgnoreCase(fkTable)) {
                if (c.getReferencedTableName().equalsIgnoreCase(refTable)) {
                    cTemp = c;
                }
            }
        }
        return cTemp;
    }

    public static Map getConstraintColumnMapByRefTable (String referencedTable, String fkTable, List<Constraint> constraints) {
        Map cTemp = new HashMap<String, String>();
        for (Constraint c: constraints) {
            if (c.getFkTableName().equalsIgnoreCase(fkTable) && c.getReferencedTableName().equalsIgnoreCase(referencedTable)){
                cTemp =  c.getConstraintColumnMap();
                break;
            }
        }
        return cTemp;
    }

    public static Constraint lookupByConstraintName(List<Constraint> constraints, String constraintName) {
        Constraint cTemp = null;
        for (Constraint c : constraints ) {
            if (c.getConstraintName().equalsIgnoreCase(constraintName)) {
                cTemp = c;
            }
        }
        return cTemp;
    }

    public static boolean containsByConstraintName(List<Constraint> constraints, String constraintName) {
        boolean exists = false;
        for (Constraint c : constraints ) {
            if (c.getConstraintName().equalsIgnoreCase(constraintName)) {
                exists = true;
            }
        }
        return exists;
    }
}
