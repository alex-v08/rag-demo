package com.atuhome.ragdemo.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;

import java.util.List;

public class PostgreSQLVectorDialect extends PostgreSQLDialect implements FunctionContributor {
    
    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry()
                .registerPattern("cosine_distance",
                        "(?1 <=> ?2)",
                        functionContributions.getTypeConfiguration()
                                .getBasicTypeRegistry()
                                .resolve(StandardBasicTypes.DOUBLE));
    }
}