package org.apache.calcite.test;

import org.apache.calcite.util.TestUtil;

import com.blzcat.adapter.util.PrintTable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class PostgresqlAdapterRemoteAvaticaTest {

    static final String URL = "jdbc:calcite:";

    private static Connection conn;


    @BeforeEach
    public void before() throws Exception {
        String resourcePatch = Objects.requireNonNull(this.getClass().getResource("/model.json"),
            "not found").getPath();

        Properties config = new Properties();
        config.put("model", resourcePatch);

        conn = DriverManager.getConnection(URL + "parserFactory=com.blzcat.adapter.AdapterSqlParserImpl#FACTORY",
            config);
    }

    @AfterEach
    public void after() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    @Test
    public void testSelect() throws SQLException {
        String sql = """
                SELECT
                    *
                FROM
                    "pg"."t1"
                """;
        try (Statement s = conn.createStatement()) {
            ResultSet r = s.executeQuery(sql);
            new PrintTable(r).printTable();
        }
    }

    @Test
    public void testEasyInsert() {
        String sql = "INSERT INTO \"pg\".\"t1\" VALUES ('sdf','abc')";
        try (Statement stmt = conn.createStatement()) {
            int i = stmt.executeUpdate(sql);
            assertEquals(i, 1);
        } catch (SQLException e) {
            throw TestUtil.rethrow(e);
        }
    }

    @Test
    public void testEasyUpdate() {
        String sql = "UPDATE \"pg\".\"t1\" SET \"c2\"='def' WHERE \"c1\"='sdf'";
        try (Statement stmt = conn.createStatement()) {
            int i = stmt.executeUpdate(sql);
            log.info("update {} count success!", i);
        } catch (SQLException e) {
            throw TestUtil.rethrow(e);
        }
    }

    @Test
    public void testEasyDelete() {
        String sql = "DELETE FROM \"pg\".\"t1\"";
        try (Statement stmt = conn.createStatement()) {
            int i = stmt.executeUpdate(sql);
            log.info("delete {} count success!", i);
        } catch (SQLException e) {
            throw TestUtil.rethrow(e);
        }
    }

    @Test
    public void testSelectError() {
        String sql = "SELECT * FROM \"gp\".\"t1\"";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            new PrintTable(rs).printTable();
        } catch (SQLException e) {
            throw TestUtil.rethrow(e);
        }
    }

    @Test
    public void testCreate() {
        String sql = """
                CREATE TABLE IF NOT EXISTS t2(
                    "ic" INTEGER NOT NULL PRIMARY KEY,
                    "vc" VARCHAR,
                    "tc" TIMESTAMP NOT NULL
                ) OWNER TO 'superuser' GROUP TO 'public' TBLPROPERTIES (
                    'ddl.appendonly.enable' = 'row'
                ) DIVIDED BY DAY 1 tc
                """;
        try (Statement stmt = conn.createStatement()) {
            int i = stmt.executeUpdate(sql);
            log.info("create {} count success!", i);
        } catch (SQLException e) {
            throw TestUtil.rethrow(e);
        }
    }

}
