package org.apache.calcite.sql.ddl;

import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public class AdapterSqlCreateTable extends SqlCreateTable {
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

    public AdapterSqlCreateTable(
        SqlParserPos pos,
        boolean replace,
        boolean ifNotExists,
        SqlIdentifier name,
        @Nullable SqlNodeList columnList,
        @Nullable SqlNode query,
        SqlNode owner,
        SqlNode group,
        SqlNodeList propertyList,
        @Nullable Integer dividedDay,
        @Nullable SqlIdentifier dividedField) {
        super(pos, replace, ifNotExists, name, columnList, query);

        this.owner = Objects.requireNonNull(owner, "owner");
        this.group = Objects.requireNonNull(group, "group");
        this.propertyList = Objects.requireNonNull(propertyList, "propertyList");
        this.dividedDay = dividedDay;
        this.dividedField = dividedField;
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        super.unparse(writer, leftPrec, rightPrec);

        for (SqlNode property : propertyList) {
            SqlTableOption propertyOpt = (SqlTableOption) property;
            String keyString = propertyOpt.getKeyString();

            if ("ddl.appendonly.enable".equals(keyString)) {
                writer.keyword("WITH (appendonly = true, orientation = 'column', compresstype = 'zlib')");
            }
        }
    }
}
