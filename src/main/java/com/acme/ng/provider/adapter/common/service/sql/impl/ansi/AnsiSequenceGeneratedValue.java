package com.acme.ng.provider.adapter.common.service.sql.impl.ansi;

import com.healthmarketscience.common.util.AppendableExt;
import com.healthmarketscience.sqlbuilder.Expression;
import com.healthmarketscience.sqlbuilder.ValidationContext;

import java.io.IOException;

public class AnsiSequenceGeneratedValue extends Expression {

    private final String sequenceName;

    public AnsiSequenceGeneratedValue(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    @Override
    public boolean hasParens() {
        return false;
    }

    @Override
    protected void collectSchemaObjects(ValidationContext vContext) {
        // empty for now
    }

    @Override
    public void appendTo(AppendableExt app) throws IOException {
        app.append("NEXT VALUE FOR " + sequenceName);
    }
}
