package org.apache.calcite.sql.ddl;

import lombok.Data;

import org.apache.calcite.jdbc.CalcitePrepare;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.jdbc.ContextSqlValidator;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.ColumnStrategy;
import org.apache.calcite.server.DdlExecutorImpl;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlWriterConfig;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.pretty.SqlPrettyWriter;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.apache.calcite.util.Pair;
import org.apache.calcite.util.Util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

public class AdapterDdlExecutor extends DdlExecutorImpl {
    @Data
    protected static class ColumnDef {
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

        public static AdapterDdlExecutor.ColumnDef of(SqlNode expr, RelDataType type,
            ColumnStrategy strategy) {
            return new AdapterDdlExecutor.ColumnDef(expr, type, strategy);
        }
    }

    protected static Pair<CalciteSchema, String> schema(CalcitePrepare.Context context,
        boolean mutable, SqlIdentifier id) {
        final String name;
        final List<String> path;
        if (id.isSimple()) {
            path = context.getDefaultSchemaPath();
            name = id.getSimple();
        } else {
            path = Util.skipLast(id.names);
            name = Util.last(id.names);
        }
        CalciteSchema schema =
            mutable ? context.getMutableRootSchema()
                : context.getRootSchema();
        for (String p : path) {
            schema = schema.getSubSchema(p, true);
        }
        return Pair.of(schema, name);
    }

    protected static void populate(SqlIdentifier name, SqlNode query,
        CalcitePrepare.Context context) {
        // Generate, prepare and execute an "INSERT INTO table query" statement.
        // (It's a bit inefficient that we convert from SqlNode to SQL and back
        // again.)
        final FrameworkConfig config = Frameworks.newConfigBuilder()
            .defaultSchema(context.getRootSchema().plus())
            .build();
        final Planner planner = Frameworks.getPlanner(config);
        try {
            final StringBuilder buf = new StringBuilder();
            final SqlWriterConfig writerConfig =
                SqlPrettyWriter.config().withAlwaysUseParentheses(false);
            final SqlPrettyWriter w = new SqlPrettyWriter(writerConfig, buf);
            buf.append("INSERT INTO ");
            name.unparse(w, 0, 0);
            buf.append(' ');
            query.unparse(w, 0, 0);
            final String sql = buf.toString();
            final SqlNode query1 = planner.parse(sql);
            final SqlNode query2 = planner.validate(query1);
            final RelRoot r = planner.rel(query2);
            final PreparedStatement prepare =
                context.getRelRunner().prepareStatement(r.rel);
            int rowCount = prepare.executeUpdate();
            Util.discard(rowCount);
            prepare.close();
        } catch (SqlParseException | ValidationException
                 | RelConversionException | SQLException e) {
            throw Util.throwAsRuntime(e);
        }
    }

    protected static SqlValidator validator(CalcitePrepare.Context context,
        boolean mutable) {
        return new ContextSqlValidator(context, mutable);
    }
}
