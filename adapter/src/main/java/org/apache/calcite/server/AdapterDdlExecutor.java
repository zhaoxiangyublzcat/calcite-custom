package org.apache.calcite.server;

import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.jdbc.CalcitePrepare;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.linq4j.Ord;
import org.apache.calcite.linq4j.function.Experimental;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rel.type.RelDataTypeImpl;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.ColumnStrategy;
import org.apache.calcite.schema.TranslatableTable;
import org.apache.calcite.schema.impl.ViewTable;
import org.apache.calcite.schema.impl.ViewTableMacro;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlUtil;
import org.apache.calcite.sql.ddl.SqlColumnDeclaration;
import org.apache.calcite.sql.ddl.SqlCreateTable;
import org.apache.calcite.sql.dialect.CalciteSqlDialect;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql2rel.InitializerContext;
import org.apache.calcite.sql2rel.InitializerExpressionFactory;
import org.apache.calcite.sql2rel.NullInitializerExpressionFactory;
import org.apache.calcite.util.Pair;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

import static org.apache.calcite.util.Static.RESOURCE;

@Experimental
public class AdapterDdlExecutor extends ServerDdlExecutor {

    public static final AdapterDdlExecutor INSTANCE = new AdapterDdlExecutor();

    private static class ColumnDef {
        final SqlNode expr;
        final RelDataType type;
        final ColumnStrategy strategy;

        private ColumnDef(SqlNode expr, RelDataType type,
            ColumnStrategy strategy) {
            this.expr = expr;
            this.type = type;
            this.strategy = Objects.requireNonNull(strategy, "strategy");
            checkArgument(
                strategy == ColumnStrategy.NULLABLE
                    || strategy == ColumnStrategy.NOT_NULLABLE
                    || expr != null);
        }

        static AdapterDdlExecutor.ColumnDef of(SqlNode expr, RelDataType type,
            ColumnStrategy strategy) {
            return new AdapterDdlExecutor.ColumnDef(expr, type, strategy);
        }
    }

    public void execute(SqlCreateTable create,
        CalcitePrepare.Context context) {
        final Pair<CalciteSchema, String> pair =
            schema(context, true, create.name);
        final JavaTypeFactory typeFactory = context.getTypeFactory();
        final RelDataType queryRowType;
        if (create.query != null) {
            // A bit of a hack: pretend it's a view, to get its row type
            final String sql =
                create.query.toSqlString(CalciteSqlDialect.DEFAULT).getSql();
            final ViewTableMacro viewTableMacro =
                ViewTable.viewMacro(pair.left.plus(), sql, pair.left.path(null),
                    context.getObjectPath(), false);
            final TranslatableTable x = viewTableMacro.apply(ImmutableList.of());
            queryRowType = x.getRowType(typeFactory);

            if (create.columnList != null
                && queryRowType.getFieldCount() != create.columnList.size()) {
                throw SqlUtil.newContextException(
                    create.columnList.getParserPosition(),
                    RESOURCE.columnCountMismatch());
            }
        } else {
            queryRowType = null;
        }
        final List<SqlNode> columnList;
        if (create.columnList != null) {
            columnList = create.columnList;
        } else {
            if (queryRowType == null) {
                // "CREATE TABLE t" is invalid; because there is no "AS query" we need
                // a list of column names and types, "CREATE TABLE t (INT c)".
                throw SqlUtil.newContextException(create.name.getParserPosition(),
                    RESOURCE.createTableRequiresColumnList());
            }
            columnList = new ArrayList<>();
            for (String name : queryRowType.getFieldNames()) {
                columnList.add(new SqlIdentifier(name, SqlParserPos.ZERO));
            }
        }
        final ImmutableList.Builder<ColumnDef> b = ImmutableList.builder();
        final RelDataTypeFactory.Builder builder = typeFactory.builder();
        final RelDataTypeFactory.Builder storedBuilder = typeFactory.builder();
        // REVIEW 2019-08-19 Danny Chan: Should we implement the
        // #validate(SqlValidator) to get the SqlValidator instance?
        final SqlValidator validator = validator(context, true);
        for (Ord<SqlNode> c : Ord.zip(columnList)) {
            if (c.e instanceof SqlColumnDeclaration) {
                final SqlColumnDeclaration d = (SqlColumnDeclaration) c.e;
                final RelDataType type = d.dataType.deriveType(validator, true);
                builder.add(d.name.getSimple(), type);
                if (d.strategy != ColumnStrategy.VIRTUAL) {
                    storedBuilder.add(d.name.getSimple(), type);
                }
                b.add(ColumnDef.of(d.expression, type, d.strategy));
            } else if (c.e instanceof SqlIdentifier) {
                final SqlIdentifier id = (SqlIdentifier) c.e;
                if (queryRowType == null) {
                    throw SqlUtil.newContextException(id.getParserPosition(),
                        RESOURCE.createTableRequiresColumnTypes(id.getSimple()));
                }
                final RelDataTypeField f = queryRowType.getFieldList().get(c.i);
                final ColumnStrategy strategy = f.getType().isNullable()
                    ? ColumnStrategy.NULLABLE
                    : ColumnStrategy.NOT_NULLABLE;
                b.add(ColumnDef.of(c.e, f.getType(), strategy));
                builder.add(id.getSimple(), f.getType());
                storedBuilder.add(id.getSimple(), f.getType());
            } else {
                throw new AssertionError(c.e.getClass());
            }
        }
        final RelDataType rowType = builder.build();
        final RelDataType storedRowType = storedBuilder.build();
        final List<ColumnDef> columns = b.build();
        final InitializerExpressionFactory ief =
            new NullInitializerExpressionFactory() {
                @Override
                public ColumnStrategy generationStrategy(RelOptTable table,
                    int iColumn) {
                    return columns.get(iColumn).strategy;
                }

                @Override
                public RexNode newColumnDefaultValue(RelOptTable table,
                    int iColumn, InitializerContext context) {
                    final ColumnDef c = columns.get(iColumn);
                    if (c.expr != null) {
                        // REVIEW Danny 2019-10-09: Should we support validation for DDL nodes?
                        final SqlNode validated = context.validateExpression(storedRowType, c.expr);
                        // The explicit specified type should have the same nullability
                        // with the column expression inferred type,
                        // actually they should be exactly the same.
                        return context.convertExpression(validated);
                    }
                    return super.newColumnDefaultValue(table, iColumn, context);
                }
            };
        if (pair.left.plus().getTable(pair.right) != null) {
            // Table exists.
            if (create.ifNotExists) {
                return;
            }
            if (!create.getReplace()) {
                // They did not specify IF NOT EXISTS, so give error.
                throw SqlUtil.newContextException(create.name.getParserPosition(),
                    RESOURCE.tableExists(pair.right));
            }
        }
        // Table does not exist. Create it.
        pair.left.add(pair.right,
            new MutableArrayTable(pair.right,
                RelDataTypeImpl.proto(storedRowType),
                RelDataTypeImpl.proto(rowType), ief));
        // todo: 实现在物理引擎创建表
        if (create.query != null) {
            populate(create.name, create.query, context);
        }
    }
}
