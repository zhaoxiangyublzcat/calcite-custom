package org.apache.calcite.test;

import org.apache.calcite.config.CalciteConnectionProperty;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
public class PostgresqlAdapterParserTest {
    static final String URL = "jdbc:calcite:";

    static final String SQL_PARSER_IMPL = "com.blzcat.adapter.AdapterSqlParserImpl#FACTORY";

    static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL,
            CalciteAssert.propBuilder()
                .set(CalciteConnectionProperty.PARSER_FACTORY,
                        SQL_PARSER_IMPL)
                .set(CalciteConnectionProperty.MATERIALIZATIONS_ENABLED,
                    "true")
                .set(CalciteConnectionProperty.FUN, "standard,postgresql")
                .build());
    }

    @Test
    void testStatement() throws Exception {
        try (Connection c = connect();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("values 1, 2")) {
            assertThat(r.next(), is(true));
            assertThat(r.getString(1), notNullValue());
            assertThat(r.next(), is(true));
            assertThat(r.next(), is(false));
        }
    }

    @Test
    void testCreateTable() throws Exception {
        try (Connection c = connect();
             Statement s = c.createStatement()) {
            String sql = """
                CREATE TABLE IF NOT EXISTS "pg"."t2"(
                    "ic" INTEGER NOT NULL PRIMARY KEY,
                    "vc" VARCHAR,
                    "tc" TIMESTAMP NOT NULL
                ) OWNER TO 'superuser' GROUP TO 'public' TBLPROPERTIES (
                    'ddl.appendonly.enable' = 'row'
                ) DIVIDED BY DAY 1 tc
                """;
            boolean b = s.execute(sql);
            assertThat(b, is(false));
            // int x = s.executeUpdate("insert into t2 values(ic) (1)");
            // assertThat(x, is(1));
            // x = s.executeUpdate("insert into t2 values(ic) (3)");
            // assertThat(x, is(1));
            // try (ResultSet r = s.executeQuery("select sum(i) from t2")) {
            //     assertThat(r.next(), is(true));
            //     assertThat(r.getInt(1), is(4));
            //     assertThat(r.next(), is(false));
            // }
        }
    }

    @Test
    void testTruncateTable() throws Exception {
        try (Connection c = connect();
             Statement s = c.createStatement()) {
            String sql = """
                create table pg.t2 (
                    i int not null
                ) OWNER TO 'superuser' GROUP TO 'public' TBLPROPERTIES (
                    'ddl.appendonly.enable' = 'row'
                ) DIVIDED BY DAY 1 i
                """;
            final boolean b = s.execute(sql);
            assertThat(b, is(false));

            final String errMsg =
                assertThrows(SQLException.class,
                    () -> s.execute("truncate table t restart identity")).getMessage();
            assertThat(errMsg, containsString("RESTART IDENTIFY is not supported"));
        }
    }
}
