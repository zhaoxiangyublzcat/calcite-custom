package com.blzcat;

import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.avatica.util.Quoting;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.impl.SqlParserImpl;

import com.blzcat.extend.ddl.CreateMaterializedView;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class MyTest {
  @Test
  public void test() throws SqlParseException {
    String sql = "CREATE MATERIALIZED VIEW IF NOT EXISTS \"test\".\"demo\"" +
        ".\"materializationName\" AS SELECT * FROM \"system\"";

    SqlParser.Config myConfig = SqlParser.config()
        .withQuoting(Quoting.DOUBLE_QUOTE)
        .withQuotedCasing(Casing.UNCHANGED)
        .withParserFactory(SqlParserImpl.FACTORY);
    SqlParser parser = SqlParser.create(sql, myConfig);
    SqlNode sqlNode = parser.parseQuery();
    assertInstanceOf(CreateMaterializedView.class, sqlNode);
    System.out.println(sqlNode);
  }
}
