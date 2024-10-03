package com.blzcat.adapter.ddl;

import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;

import org.checkerframework.checker.nullness.qual.Nullable;

public class SqlCreateTable extends org.apache.calcite.sql.ddl.SqlCreateTable {

    protected SqlCreateTable(SqlParserPos pos, boolean replace, boolean ifNotExists, SqlIdentifier name,
                             @Nullable SqlNodeList columnList, @Nullable SqlNode query) {
        super(pos, replace, ifNotExists, name, columnList, query);
    }

    public SqlCreateTable(SqlParserPos pos, boolean replace, boolean ifNotExists, SqlIdentifier name,
                          @Nullable SqlNodeList columnList, @Nullable SqlNode query, SqlNode owner, SqlNode group,
                          SqlNodeList propertyList, @Nullable Integer dividedDay, @Nullable SqlIdentifier dividedField) {
        super(pos, replace, ifNotExists, name, columnList, query, owner, group, propertyList, dividedDay, dividedField);
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

        for (SqlNode property : propertyList) {
            SqlTableOption propertyOpt = (SqlTableOption) property;
            String keyString = propertyOpt.getKeyString();
            String valueString = propertyOpt.getValueString();

            if ("ddl.appendonly.enable" .equals(keyString)) {
                writer.keyword("WITH (appendonly = true, orientation = 'column', compresstype = 'zlib')");
            }
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
