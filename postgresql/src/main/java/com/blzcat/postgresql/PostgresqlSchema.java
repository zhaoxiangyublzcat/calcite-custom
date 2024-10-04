package com.blzcat.postgresql;

import org.apache.calcite.adapter.jdbc.JdbcConvention;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.sql.SqlDialect;

import org.checkerframework.checker.nullness.qual.Nullable;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PostgresqlSchema extends JdbcSchema {

    public PostgresqlSchema(DataSource dataSource, SqlDialect dialect, JdbcConvention convention,
        @Nullable String catalog, @Nullable String schema) {
        super(dataSource, dialect, convention, catalog, schema);
    }
}
