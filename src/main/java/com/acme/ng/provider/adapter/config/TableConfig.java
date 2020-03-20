package com.acme.ng.provider.adapter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties("tables")
public class TableConfig {
    private List<ParentModel> list;

    public List<ParentModel> getList() { return list; }
    public void setList(List<ParentModel> list) {this.list = list; }

    public static class ParentModel {
        private String name;
        private List<ForeignKey> foreign_keys;

        public void setName(String name) {this.name = name; }
        public String getName() {return name; }

        public List<ForeignKey> getForeign_keys() {
            return foreign_keys;
        }

        public void setForeign_keys(List<ForeignKey> foreign_keys) {this.foreign_keys = foreign_keys; }

        public static class ForeignKey {
            private List<String> fk_column_names;
            private List<String> reference_column_names;
            private List<ParentModel> reference_table;

            public List<String> getFk_column_names() { return fk_column_names;}

            public void setFk_column_names(List<String> fk_column_names) {
                this.fk_column_names = fk_column_names;
            }

            public List<String> getReference_column_names() { return reference_column_names; }

            public void setReference_column_names(List<String> reference_column_names) {
                this.reference_column_names = reference_column_names;
            }

            public List<ParentModel> getReference_table() { return reference_table; }

            public void setReference_table(List<ParentModel> reference_table) {
                this.reference_table = reference_table;
            }
        }
    }
}
