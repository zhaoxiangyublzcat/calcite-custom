boolean IfNotExistsOpt() :
{
}
{
    <IF> <NOT> <EXISTS> { return true; }
|
    { return false; }
}

SqlNodeList TableElementList() :
{
    final Span s;
    final List<SqlNode> list = new ArrayList<SqlNode>();
}
{
    <LPAREN> { s = span(); }
    TableElement(list)
    (
        <COMMA> TableElement(list)
    )*
    <RPAREN> {
        return new SqlNodeList(list, s.end(this));
    }
}

void TableElement(List<SqlNode> list) :
{
    final SqlIdentifier id;
    final SqlDataTypeSpec type;
    final boolean nullable;
    final SqlNode e;
    SqlIdentifier name = null;
    final SqlNodeList columnList;
    final Span s = Span.of();
    final ColumnStrategy strategy;
    boolean ifPrimaryKey = false;
}
{
    LOOKAHEAD(2) id = SimpleIdentifier()
    (
        type = DataType()
        nullable = NullableOptDefaultTrue()
        (
            [ <GENERATED> <ALWAYS> ] <AS> <LPAREN>
            e = Expression(ExprContext.ACCEPT_SUB_QUERY) <RPAREN>
            (
                <VIRTUAL> { strategy = ColumnStrategy.VIRTUAL; }
            |
                <STORED> { strategy = ColumnStrategy.STORED; }
            |
                { strategy = ColumnStrategy.VIRTUAL; }
            )
        |
            <DEFAULT_> e = Expression(ExprContext.ACCEPT_SUB_QUERY) {
                strategy = ColumnStrategy.DEFAULT;
            }
        |
            {
                e = null;
                strategy = nullable ? ColumnStrategy.NULLABLE
                    : ColumnStrategy.NOT_NULLABLE;
            }
        )
        (
            <PRIMARY> <KEY> {
                ifPrimaryKey = true;
            }
        )*
        {
            list.add(
                SqlDdlNodes.column(s.add(id).end(this), id,
                    type.withNullable(nullable), e, strategy, ifPrimaryKey));
        }
    |
        { list.add(id); }
    )
|
    id = SimpleIdentifier() {
        list.add(id);
    }
|
    [ <CONSTRAINT> { s.add(this); } name = SimpleIdentifier() ]
    (
        <CHECK> { s.add(this); } <LPAREN>
        e = Expression(ExprContext.ACCEPT_SUB_QUERY) <RPAREN> {
            list.add(SqlDdlNodes.check(s.end(this), name, e));
        }
    |
        <UNIQUE> { s.add(this); }
        columnList = ParenthesizedSimpleIdentifierList() {
            list.add(SqlDdlNodes.unique(s.end(columnList), name, columnList));
        }
    |
        <PRIMARY>  { s.add(this); } <KEY>
        columnList = ParenthesizedSimpleIdentifierList() {
            list.add(SqlDdlNodes.primary(s.end(columnList), name, columnList));
        }
    )
}

SqlCreate SqlCreateTableLike(Span s, boolean replace, boolean ifNotExists, SqlIdentifier id) :
{
    final SqlIdentifier sourceTable;
    final boolean likeOptions;
    final SqlNodeList including = new SqlNodeList(getPos());
    final SqlNodeList excluding = new SqlNodeList(getPos());
}
{
    sourceTable = CompoundIdentifier()
    [ LikeOptions(including, excluding) ]
    {
        return SqlDdlNodes.createTableLike(s.end(this), replace, ifNotExists, id, sourceTable, including, excluding);
    }
}

void LikeOptions(SqlNodeList including, SqlNodeList excluding) :
{
}
{
    LikeOption(including, excluding)
    (
        LikeOption(including, excluding)
    )*
}

void LikeOption(SqlNodeList includingOptions, SqlNodeList excludingOptions) :
{
    boolean including = false;
    SqlCreateTableLike.LikeOption option;
}
{
    (
        <INCLUDING> { including = true; }
    |
        <EXCLUDING> { including = false; }
    )
    (
        <ALL> { option = SqlCreateTableLike.LikeOption.ALL; }
    |
        <DEFAULTS> { option = SqlCreateTableLike.LikeOption.DEFAULTS; }
    |
        <GENERATED> { option = SqlCreateTableLike.LikeOption.GENERATED; }
    )
    {
        if (including) {
            includingOptions.add(option.symbol(getPos()));
        } else {
            excludingOptions.add(option.symbol(getPos()));
        }
    }
}

SqlNodeList TableProperties():
{
    SqlNode property;
    final List<SqlNode> proList = new ArrayList<SqlNode>();
    final Span span;
}
{
    <LPAREN> { span = span(); }
    [
        property = TableOption() { proList.add(property); }
        (
            <COMMA> property = TableOption() { proList.add(property); }
        )*
    ]
    <RPAREN>
    {  return new SqlNodeList(proList, span.end(this)); }
}

SqlNode TableOption() :
{
    SqlNode key;
    SqlNode value;
    SqlParserPos pos;
}
{
    key = StringLiteral()
    {
        pos = getPos();
    }
    <EQ> value = StringLiteral()
    {
        return new SqlTableOption(key, value, getPos());
    }
}

SqlCreate CreateTable(Span s, boolean replace) :
{
    final boolean ifNotExists;
    final SqlIdentifier id;
    SqlNodeList tableElementList = null;
    SqlNode query = null;
    SqlNode owner;
    SqlNode group;
    SqlNodeList propertyList = null;
    Integer dividedDay;
    SqlIdentifier dividedField;

    SqlCreate createTableLike = null;
}
{
    <TABLE> ifNotExists = IfNotExistsOpt() id = CompoundIdentifier()
    (
        <LIKE> createTableLike = SqlCreateTableLike(s, replace, ifNotExists, id) {
            return createTableLike;
        }
    |
        [ tableElementList = TableElementList() ]
        [ <AS> query = OrderedQueryOrExpr(ExprContext.ACCEPT_QUERY) ]
        <OWNER> <TO> owner = StringLiteral()
        <GROUP> <TO> group = StringLiteral()
        <TBLPROPERTIES> propertyList = TableProperties()
        <DIVIDED> <BY> <DAY> dividedDay = UnsignedIntLiteral() dividedField = SimpleIdentifier()
        {
            return new PostgresqlSqlCreateTable(s.end(this), replace, ifNotExists, id, tableElementList, query, owner, group,
            propertyList, dividedDay, dividedField);
        }
    )
}
