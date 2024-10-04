package org.apache.calcite.sql.ddl;

import org.apache.calcite.schema.ColumnStrategy;
import org.apache.calcite.sql.SqlDataTypeSpec;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;

import org.checkerframework.checker.nullness.qual.Nullable;

public class AdapterSqlColumnDeclaration extends SqlColumnDeclaration {
    /**
     * 是否是主键
     */
    public boolean ifPrimaryKey;

    public AdapterSqlColumnDeclaration(
        SqlParserPos pos,
        SqlIdentifier name,
        SqlDataTypeSpec dataType,
        @Nullable SqlNode expression,
        ColumnStrategy strategy,
        boolean ifPrimaryKey) {
        super(pos, name, dataType, expression, strategy);

        this.ifPrimaryKey = ifPrimaryKey;
    }

    @Override
    public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
        super.unparse(writer, leftPrec, rightPrec);

        if (Boolean.TRUE.equals(ifPrimaryKey)) {
            writer.keyword("PRIMARY KEY");
        }
    }
}
