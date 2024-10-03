package org.apache.calcite.test;

import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.jdbc.JdbcCatalogSchema;
import org.apache.calcite.adapter.jdbc.JdbcConvention;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Schemas;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;
import org.apache.calcite.util.BuiltInMethod;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;
import javax.sql.DataSource;

import static java.util.Objects.requireNonNull;

public class PostgresqlSchemaFactory implements SchemaFactory {

    @Override
    public Schema create(SchemaPlus parentSchema, String name, Map<String, Object> operand) {
        final DataSource dataSource = initDataSource(operand);
        final Expression expression =
            parentSchema != null
                ? Schemas.subSchemaExpression(parentSchema, name,
                JdbcCatalogSchema.class)
                : Expressions.call(DataContext.ROOT,
                    BuiltInMethod.DATA_CONTEXT_GET_ROOT_SCHEMA.method);
        final SqlDialect dialect = PostgresqlSqlDialect.DEFAULT;
        final JdbcConvention convention = JdbcConvention.of(dialect, expression, name);

        return new PostgresqlSchema(dataSource, dialect, convention, null, "public");
    }

    private DataSource initDataSource(Map<String, Object> operand) {
        Object jdbcDriver = operand.get("jdbcDriver");
        Object jdbcUrl = operand.get("jdbcUrl");
        Object jdbcUser = operand.get("jdbcUser");
        Object jdbcPassword = operand.get("jdbcPassword");

        requireNonNull(jdbcDriver, "jdbcDriver must be specified");
        requireNonNull(operand.get("jdbcUrl"), "jdbcUrl must be specified");
        requireNonNull(operand.get("jdbcUser"), "jdbcUser must be specified");
        requireNonNull(operand.get("jdbcPassword"), "jdbcPassword must be specified");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl.toString());
        config.setUsername(jdbcUser.toString());
        config.setPassword(jdbcPassword.toString());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("keepaliveTime", "60000");
        config.addDataSourceProperty("connectionTestQuery", "select 1");
        config.addDataSourceProperty("maximumPoolSize", 24 * 2);

        return new HikariDataSource(config);
    }
}
