package com.blzcat.adapter.ddl;// package com.blzcat.adapter.postgresql.ddl;

import org.apache.calcite.jdbc.CalcitePrepare;
import org.apache.calcite.server.ServerDdlExecutor;

public class SqlDdlExecutor extends ServerDdlExecutor {
    public void execute(SqlCreateTable create, CalcitePrepare.Context context) {
        throw new UnsupportedOperationException("CREATE FUNCTION is not supported");
    }
}
