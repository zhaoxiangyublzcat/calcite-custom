package com.blzcat.adapter.postgresql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class PostgresqlDatasourcePolicy implements AfterAllCallback {
    private final HikariDataSource dataSource;

    private PostgresqlDatasourcePolicy(Map<String, Object> operand) throws SQLException {
        dataSource = initDataSource(operand);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        dataSource.close();
    }

    static PostgresqlDatasourcePolicy create() {
        try {
            Map<String, Object> operand = new HashMap<>();
            return new PostgresqlDatasourcePolicy(operand);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    HikariDataSource dataSource() {
        return dataSource;
    }

    private HikariDataSource initDataSource(Map<String, Object> operand) {
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
