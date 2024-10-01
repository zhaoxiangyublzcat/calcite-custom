package com.blzcat.adapter.postgresql;

import org.apache.calcite.util.TestUtil;

import com.blzcat.extend.util.PrintTable;

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
public class PostgresqlAdapterTest {
    private static Connection conn;

    @BeforeEach
    public void before() throws Exception {
        String resourcePatch = Objects.requireNonNull(this.getClass().getResource("/"),
            "not found").getPath();

        Properties config = new Properties();
        config.put("model", resourcePatch + "model.yaml");

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
        String sql = "SELECT * FROM \"PG\".\"t1\"";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            new PrintTable(rs).printTable();
        } catch (SQLException e) {
            throw TestUtil.rethrow(e);
        }
    }

    @Test
    public void testEasyInsert() {
        String sql = "INSERT INTO \"PG\".\"t1\" VALUES ('sdf','abc')";
        try (Statement stmt = conn.createStatement()) {
            int i = stmt.executeUpdate(sql);
            assertEquals(i, 1);
        } catch (SQLException e) {
            throw TestUtil.rethrow(e);
        }
    }

    @Test
    public void testEasyUpdate() {
        String sql = "UPDATE \"PG\".\"t1\" SET \"c2\"='def' WHERE \"c1\"='sdf'";
        try (Statement stmt = conn.createStatement()) {
            int i = stmt.executeUpdate(sql);
            log.info("update {} count success!", i);
        } catch (SQLException e) {
            throw TestUtil.rethrow(e);
        }
    }

    @Test
    public void testEasyDelete() {
        String sql = "DELETE FROM \"PG\".\"t1\"";
        try (Statement stmt = conn.createStatement()) {
            int i = stmt.executeUpdate(sql);
            log.info("delete {} count success!", i);
        } catch (SQLException e) {
            throw TestUtil.rethrow(e);
        }
    }

}
