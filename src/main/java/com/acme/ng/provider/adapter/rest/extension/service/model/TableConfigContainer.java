package com.acme.ng.provider.adapter.rest.extension.service.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableConfigContainer {

    private Map<String, List<Map<String,Object>>> mapOfTableDatas;
    private List<Constraint> constraintList;
    private Map<String, List<String>> constraintMap;


    public TableConfigContainer() {

        mapOfTableDatas = new HashMap<>();
        constraintList = new ArrayList<>();
        constraintMap = new HashMap<>();

    }

    public TableConfigContainer(Map<String, List<Map<String, Object>>> mapOfTableDatas, List<Constraint> constraintList, Map<String, List<String>> constraintMap) {
        this.mapOfTableDatas = mapOfTableDatas;
        this.constraintList = constraintList;
        this.constraintMap = constraintMap;
    }




    public Map<String, List<Map<String, Object>>> getMapOfTableDatas() {
        return mapOfTableDatas;
    }

    public void setMapOfTableDatas(Map<String, List<Map<String, Object>>> mapOfTableDatas) {
        this.mapOfTableDatas = mapOfTableDatas;
    }

    public List<Constraint> getConstraintList() {
        return constraintList;
    }

    public void setConstraintList(List<Constraint> constraintList) {
        this.constraintList = constraintList;
    }

    public Map<String, List<String>> getConstraintMap() {
        return constraintMap;
    }

    public void setConstraintMap(Map<String, List<String>> constraintMap) {
        this.constraintMap = constraintMap;
    }


}
