package com.acme.ng.provider.adapter.common.service.sql.impl.postgres;

import com.healthmarketscience.common.util.AppendableExt;
import com.healthmarketscience.sqlbuilder.Expression;
import com.healthmarketscience.sqlbuilder.ValidationContext;

import java.io.IOException;

/**
 * Created 4/30/2018.
 */
public class PostgresSequenceGeneratedValue extends Expression {

    private final String sequenceName;

    public PostgresSequenceGeneratedValue(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    @Override
    public boolean hasParens() {
        return false;
    }

    @Override
    protected void collectSchemaObjects(ValidationContext vContext) {
        //TODO empty for now
    }

    @Override
    public void appendTo(AppendableExt app) throws IOException {
        app.append("nextval('" + sequenceName + "')");
    }
}
