package com.blzcat;

import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.avatica.util.Quoting;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;

import com.blzcat.adapter.AdapterSqlParserImpl;
import org.apache.calcite.sql.ddl.AdapterSqlCreateTable;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Slf4j
public class SqlParserTest {
    @Test
    public void test() throws SqlParseException {
        String sql = """
                CREATE TABLE IF NOT EXISTS pg.t2(
                    "ic" INTEGER NOT NULL PRIMARY KEY,
                    "vc" VARCHAR,
                    "tc" TIMESTAMP NOT NULL
                ) OWNER TO 'superuser' GROUP TO 'public' TBLPROPERTIES (
                    'ddl.appendonly.enable' = 'row'
                ) DIVIDED BY DAY 1 tc
                """;
        SqlParser.Config myConfig = SqlParser.config()
            .withQuoting(Quoting.DOUBLE_QUOTE)
            .withQuotedCasing(Casing.UNCHANGED)
            .withParserFactory(AdapterSqlParserImpl.FACTORY)
            .withCaseSensitive(false);
        SqlParser parser = SqlParser.create(sql, myConfig);
        SqlNode sqlNode = parser.parseQuery();
        assertInstanceOf(AdapterSqlCreateTable.class, sqlNode);
        System.out.println(sqlNode);
    }
}
