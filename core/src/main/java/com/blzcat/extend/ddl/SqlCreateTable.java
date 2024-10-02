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
package com.blzcat.extend.ddl;

import org.apache.calcite.sql.SqlCreate;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.util.ImmutableNullableList;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Parse tree for {@code CREATE TABLE} statement.
 */
public class SqlCreateTable extends SqlCreate {
    public final SqlIdentifier name;
    public final @Nullable SqlNodeList columnList;
    public final @Nullable SqlNode query;

    /**
     * 所属用户
     */
    public SqlNode owner;

    /**
     * 所属组
     */
    public SqlNode group;

    /**
     * 表属性配置
     */
    public SqlNodeList propertyList;

    /**
     * 留存期量
     */
    public @Nullable Integer dividedDay;

    /**
     * 留存期字段
     */
    public @Nullable SqlIdentifier dividedField;

    private static final SqlOperator OPERATOR =
        new SqlSpecialOperator("CREATE TABLE", SqlKind.CREATE_TABLE);

    /**
     * Creates a SqlCreateTable.
     */
    public SqlCreateTable(SqlParserPos pos, boolean replace, boolean ifNotExists,
        SqlIdentifier name, @Nullable SqlNodeList columnList, @Nullable SqlNode query,
        SqlNode owner,
        SqlNode group,
        SqlNodeList propertyList,
        @Nullable Integer dividedDay,
        @Nullable SqlIdentifier dividedField) {
        super(OPERATOR, pos, replace, ifNotExists);
        this.name = Objects.requireNonNull(name, "name");
        this.columnList = columnList; // may be null
        this.query = query; // for "CREATE TABLE ... AS query"; may be null
        this.owner = Objects.requireNonNull(owner, "owner");
        this.group = Objects.requireNonNull(group, "group");
        this.propertyList = Objects.requireNonNull(propertyList, "propertyList");
        this.dividedDay = dividedDay;
        this.dividedField = dividedField;
    }

    @SuppressWarnings("nullness")
    @Override
    public List<SqlNode> getOperandList() {
        return ImmutableNullableList.of(name, columnList, query);
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        writer.keyword("CREATE");
        writer.keyword("TABLE");
        if (ifNotExists) {
            writer.keyword("IF NOT EXISTS");
        }
        name.unparse(writer, leftPrec, rightPrec);

        unparseColumnList(writer);

        if (query != null) {
            writer.keyword("AS");
            writer.newlineAndIndent();
            query.unparse(writer, 0, 0);
        }

        writer.keyword("OWNER TO");
        owner.unparse(writer, 0, 0);

        writer.keyword("GROUP TO");
        owner.unparse(writer, 0, 0);

        writer.keyword("TBLPROPERTIES");
        SqlWriter.Frame frame = writer.startList("(", ")");
        for (SqlNode property : propertyList) {
            writer.sep(",");
            property.unparse(writer, 0, 0);
        }
        writer.endList(frame);

        if (dividedField != null && dividedDay != null) {
            writer.keyword("DIVIDED BY DAY");
            dividedField.unparse(writer, 0, 0);
        }
    }

    private void unparseColumnList(SqlWriter writer) {
        if (columnList != null) {
            SqlWriter.Frame frame = writer.startList("(", ")");
            for (SqlNode c : columnList) {
                writer.sep(",");
                c.unparse(writer, 0, 0);
            }
            writer.endList(frame);
        }
    }
}
