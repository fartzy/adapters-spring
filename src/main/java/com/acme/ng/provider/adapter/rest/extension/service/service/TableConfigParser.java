package com.acme.ng.provider.adapter.rest.extension.service.service;

import com.acme.ng.provider.adapter.config.TableConfig;
import com.acme.ng.provider.adapter.rest.extension.service.model.Constraint;
import com.acme.ng.provider.adapter.rest.extension.service.model.TableConfigContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TableConfigParser {
    @Autowired
    private TableConfig tableConfig;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TableConfigParser.class);
        app.run();
    }

    public TableConfigContainer parse() {

        Map<String, List<String>> tblMap = new HashMap<String, List<String>>();
        List<Constraint> constraintList = new ArrayList<Constraint>();

        //iterate through each parentModel
        for ( TableConfig.ParentModel model : tableConfig.getList()) {

            //for each foreign key model
            for ( TableConfig.ParentModel.ForeignKey foreignKey: model.getForeign_keys()) {
                tblMap.put(model.getName(), (tblMap.containsKey(model.getName())
                        ? addToList(tblMap.get(model.getName()), foreignKey.getReference_table().get(0).getName())
                        : new ArrayList<String>() {{ add(foreignKey.getReference_table().get(0).getName()); }} ));
                for (int i = 0; i < foreignKey.getFk_column_names().size(); i++) {
                    if  (Constraint.lookupByFKTableAndRefTable(constraintList, model.getName(), foreignKey.getReference_table().get(0).getName()) == null ) {
                        constraintList.add(new Constraint(model.getName() + "_" + foreignKey.getReference_table().get(0).getName(),
                                model.getName(),
                                foreignKey.getFk_column_names().get(i),
                                foreignKey.getReference_table().get(0).getName(),
                                foreignKey.getReference_column_names().get(i))); }
                    else {Constraint.lookupByFKTableAndRefTable(constraintList, model.getName(), foreignKey.getReference_table().get(0).getName())
                            .addToConstraintColumnMap(foreignKey.getFk_column_names().get(i), foreignKey.getReference_column_names().get(i)); }
                }// end for
            } //end for
        }// end for

        return new TableConfigContainer(new HashMap<String, List<Map<String, Object>>>(), constraintList, tblMap);
    }// end run


    public List<String> addToList(List<String> lst, String t) {
        lst.add(t);
        return lst;
    }
}
