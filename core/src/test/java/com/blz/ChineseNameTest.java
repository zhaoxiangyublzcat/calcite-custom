/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blz;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeSystemImpl;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.type.BasicSqlType;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.tools.Frameworks;

import org.junit.jupiter.api.Test;


class ChineseNameTest {

  @Test
  void test1() throws SqlParseException {
    String sql = "" +
        " select " +
        "   * " +
        " from " +
        "   users";

    SchemaPlus rootSchema = Frameworks.createRootSchema(true);
    rootSchema.add("USERS", new AbstractTable() { // note: add a table
      @Override
      public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
        RelDataTypeFactory.Builder builder = typeFactory.builder();

        builder.add("ID", new BasicSqlType(new RelDataTypeSystemImpl() {
        }, SqlTypeName.INTEGER), "用户ID");
        builder.add("NAME", new BasicSqlType(new RelDataTypeSystemImpl() {
        }, SqlTypeName.CHAR), "名字");
        builder.add("AGE", new BasicSqlType(new RelDataTypeSystemImpl() {
        }, SqlTypeName.INTEGER), "年龄");
        return builder.build();
      }
    });

    SqlParser parser = SqlParser.create(sql, SqlParser.Config.DEFAULT);
    SqlNode sqlNode = parser.parseStmt();
  }
}
