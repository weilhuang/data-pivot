package com.data.pivot.plugin.tool;

import com.data.pivot.plugin.entity.DatabaseQueryConfig;
import com.data.pivot.plugin.enums.DBType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class QueryToolTest {
    @Test
    public void addUrlParametersUsesQuestionMarkWhenUrlHasNoQuery() {
        String url = QueryTool.addUrlParameters("jdbc:mysql://localhost:3306/demo", "useUnicode=true");

        assertEquals("jdbc:mysql://localhost:3306/demo?useUnicode=true", url);
    }

    @Test
    public void addUrlParametersUsesAmpersandWhenUrlAlreadyHasQuery() {
        String url = QueryTool.addUrlParameters("jdbc:mysql://localhost:3306/demo?serverTimezone=UTC", "useUnicode=true");

        assertEquals("jdbc:mysql://localhost:3306/demo?serverTimezone=UTC&useUnicode=true", url);
    }

    @Test
    public void generateMysqlSqlWithLikeCondition() {
        DatabaseQueryConfig config = config(DBType.MYSQL, List.of("id", "name"), "name", "alice");

        String sql = QueryTool.generateSql(config);

        assertEquals("SELECT id, name FROM demo.user_account WHERE name LIKE ? LIMIT 20", sql);
    }

    @Test
    public void generateOracleSqlWithoutLikeConditionAddsWhereBeforeRowNum() {
        DatabaseQueryConfig config = config(DBType.ORACLE, List.of("*"), null, null);

        String sql = QueryTool.generateSql(config);

        assertEquals("SELECT * FROM demo.user_account WHERE ROWNUM <= 20", sql);
    }

    @Test
    public void generateOracleSqlWithLikeConditionAddsAndBeforeRowNum() {
        DatabaseQueryConfig config = config(DBType.ORACLE, List.of("id"), "id", "42");

        String sql = QueryTool.generateSql(config);

        assertEquals("SELECT id FROM demo.user_account WHERE id LIKE ? AND ROWNUM <= 20", sql);
    }

    @Test
    public void generatePostgresSqlWithLikeCondition() {
        DatabaseQueryConfig config = config(DBType.POSTGRES, List.of("id", "name"), "name", "alice");

        String sql = QueryTool.generateSql(config);

        assertEquals("SELECT id, name FROM demo.user_account WHERE name LIKE ? LIMIT 20", sql);
    }

    @Test
    public void isDirectSqlQueryWhenSqlIsPresent() {
        DatabaseQueryConfig config = config(DBType.MYSQL, List.of("*"), null, null);
        assertEquals(false, config.isDirectSqlQuery());
        config.setSql("SELECT 1");
        assertEquals(true, config.isDirectSqlQuery());
    }

    @Test
    public void generateSqlServerSqlUsesSchemaWhenPresent() {
        DatabaseQueryConfig config = new DatabaseQueryConfig(
                "ds",
                DBType.MSSQL,
                "jdbc:sqlserver://localhost:1433;databaseName=demo",
                "user",
                "password",
                "driver",
                List.of(),
                "demo",
                "dbo",
                "user_account",
                List.of("id"),
                null,
                null,
                null
        );

        String sql = QueryTool.generateSql(config);

        assertEquals("SELECT TOP 20 id FROM demo.dbo.user_account", sql);
    }

    private static DatabaseQueryConfig config(DBType dbType, List<String> columns, String conditionField, String likeValue) {
        return new DatabaseQueryConfig(
                "ds",
                dbType,
                "jdbc://localhost/demo",
                "user",
                "password",
                "driver",
                List.of(),
                "demo",
                "",
                "user_account",
                columns,
                conditionField,
                likeValue,
                null
        );
    }
}
