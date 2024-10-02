package com.blzcat.adapter.postgresql;

import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.avatica.util.Quoting;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.ddl.SqlCreateTable;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.impl.SqlParserImpl;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Slf4j
public class PostgresqlAdapterParserTest {
    @Test
    public void test() throws SqlParseException {
        String sql =
            new StringBuilder()
                .append("CREATE TABLE IF NOT EXISTS pg.t2(\n")
                .append("  \"ic\" INTEGER NOT NULL PRIMARY KEY,\n")
                .append("  \"vc\" VARCHAR,\n")
                .append("  \"tc\" TIMESTAMP NOT NULL\n")
                .append(") OWNER TO 'superuser' GROUP TO 'public' TBLPROPERTIES (\n")
                .append("  'ddl.appendonly.enable' = 'row'\n")
                .append(") DIVIDED BY DAY 1 tc").toString();

        SqlParser.Config myConfig = SqlParser.config()
            .withQuoting(Quoting.DOUBLE_QUOTE)
            .withQuotedCasing(Casing.UNCHANGED)
            .withParserFactory(SqlParserImpl.FACTORY)
            .withCaseSensitive(false);
        SqlParser parser = SqlParser.create(sql, myConfig);
        SqlNode sqlNode = parser.parseQuery();
        assertInstanceOf(SqlCreateTable.class, sqlNode);
        System.out.println(sqlNode);
    }

}
