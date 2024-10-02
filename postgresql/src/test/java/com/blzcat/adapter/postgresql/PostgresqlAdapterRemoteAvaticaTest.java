package com.blzcat.adapter.postgresql;

import org.apache.calcite.util.TestUtil;

import com.blzcat.util.PrintTable;

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
    private static Connection conn;

    @BeforeEach
    public void before() throws Exception {
        String resourcePatch = Objects.requireNonNull(this.getClass().getResource("/"),
            "not found").getPath();

        Properties config = new Properties();
        config.put("model", resourcePatch + "model.json");

        conn = DriverManager.getConnection("jdbc:calcite:", config);
    }

    @AfterEach
    public void after() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    @Test
    public void testEasySelect() {
        String sql = "SELECT * FROM \"pg\".\"t1\"";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            new PrintTable(rs).printTable();
        } catch (SQLException e) {
            throw TestUtil.rethrow(e);
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
        String sql = new StringBuilder()
            .append("CREATE TABLE IF NOT EXISTS pg.t2(\n")
            .append("  `ic` INTEGER NOT NULL PRIMARY KEY,\n")
            .append("  `vc` VARCHAR,\n")
            .append("  `tc` TIMESTAMP NOT NULL\n")
            .append(") OWNER TO 'superuser' GROUP TO 'public' TBLPROPERTIES (\n")
            .append("  'ddl.appendonly.enable' = 'row'\n")
            .append(") DIVIDED BY DAY 1 tc;")
            .toString();
        try (Statement stmt = conn.createStatement()) {
            int i = stmt.executeUpdate(sql);
            log.info("create {} count success!", i);
        } catch (SQLException e) {
            throw TestUtil.rethrow(e);
        }
    }

}
