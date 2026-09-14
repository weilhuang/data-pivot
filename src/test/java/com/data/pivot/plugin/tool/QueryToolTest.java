package com.data.pivot.plugin.tool;

import com.data.pivot.plugin.entity.DatabaseQueryConfig;
import com.data.pivot.plugin.enums.DBType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    public void generatePostgresSqlUsesSchemaWhenPresentAndDbDiffers() {
        DatabaseQueryConfig config = new DatabaseQueryConfig(
                "ds",
                DBType.POSTGRES,
                "jdbc:postgresql://localhost:5432/demo",
                "user",
                "password",
                "driver",
                List.of(),
                "demo",
                "public",
                "user_account",
                List.of("id", "name"),
                "name",
                "alice",
                null
        );

        String sql = QueryTool.generateSql(config);

        assertEquals("SELECT id, name FROM public.user_account WHERE name LIKE ? LIMIT 20", sql);
        assertEquals("\"public\".\"user_account\"", QueryTool.quotedTableRef(config));
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

    @Test
    public void generateMysqlAnalysisSqlQuotesIdentifiersAndLimitsDistinctValues() {
        DatabaseQueryConfig config = config(DBType.MYSQL, List.of("name"), "name", null);

        String sql = QueryTool.generateAnalysisSql(config);

        assertEquals(
                "SELECT `name`, COUNT(*) AS rs_count, ROUND(COUNT(*) * 100.0 / NULLIF((SELECT COUNT(*) FROM `demo`.`user_account`), 0), 2) AS percentage FROM `demo`.`user_account` GROUP BY `name` ORDER BY rs_count DESC, percentage DESC LIMIT 20",
                sql);
    }

    @Test
    public void generatePostgresAnalysisSqlUsesQuotedIdentifiers() {
        DatabaseQueryConfig config = config(DBType.POSTGRES, List.of("name"), "name", null);

        String sql = QueryTool.generateAnalysisSql(config);

        assertEquals(
                "SELECT \"name\", COUNT(*) AS rs_count, ROUND(COUNT(*) * 100.0 / NULLIF((SELECT COUNT(*) FROM \"demo\".\"user_account\"), 0), 2) AS percentage FROM \"demo\".\"user_account\" GROUP BY \"name\" ORDER BY rs_count DESC, percentage DESC LIMIT 20",
                sql);
    }

    @Test
    public void generatePostgresAnalysisSqlPrefersSchemaOverDatabaseName() {
        DatabaseQueryConfig config = new DatabaseQueryConfig(
                "ds",
                DBType.POSTGRES,
                "jdbc:postgresql://localhost:5432/demo",
                "user",
                "password",
                "driver",
                List.of(),
                "demo",
                "public",
                "user_account",
                List.of("name"),
                "name",
                null,
                null
        );

        String sql = QueryTool.generateAnalysisSql(config);

        assertEquals(
                "SELECT \"name\", COUNT(*) AS rs_count, ROUND(COUNT(*) * 100.0 / NULLIF((SELECT COUNT(*) FROM \"public\".\"user_account\"), 0), 2) AS percentage FROM \"public\".\"user_account\" GROUP BY \"name\" ORDER BY rs_count DESC, percentage DESC LIMIT 20",
                sql);
    }

    @Test
    public void generateOracleAnalysisSqlQuotesAliasesExpectedByResultModel() {
        DatabaseQueryConfig config = config(DBType.ORACLE, List.of("name"), "name", null);

        String sql = QueryTool.generateAnalysisSql(config);

        assertEquals(
                "SELECT * FROM (SELECT \"name\", COUNT(*) AS \"rs_count\", ROUND(COUNT(*) * 100.0 / NULLIF((SELECT COUNT(*) FROM \"demo\".\"user_account\"), 0), 2) AS \"percentage\" FROM \"demo\".\"user_account\" GROUP BY \"name\" ORDER BY COUNT(*) DESC) WHERE ROWNUM <= 20",
                sql);
        assertTrue(sql.contains("AS \"" + com.data.pivot.plugin.view.report.AnalysisResultModel.COUNT_COLUMN + "\""));
        assertTrue(sql.contains("AS \"" + com.data.pivot.plugin.view.report.AnalysisResultModel.PERCENTAGE_COLUMN + "\""));
        assertEquals("\"rs_count\"", QueryTool.analysisAlias(DBType.ORACLE, QueryTool.ANALYSIS_COUNT_ALIAS));
    }

    @Test
    public void generateAnalysisSqlThrowsQueryFailedExceptionForMongo() {
        DatabaseQueryConfig config = config(DBType.MONGO, List.of("name"), "name", null);
        try {
            QueryTool.generateAnalysisSql(config);
            fail("MongoDB analysis must fail with a user-facing QueryFailedException");
        } catch (QueryFailedException expected) {
            assertTrue(expected.getMessage().contains("MONGO"));
        }
    }

    @Test
    public void generateSqlServerAnalysisSqlUsesTopAndBracketQuotes() {
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
                List.of("name"),
                "name",
                null,
                null
        );

        String sql = QueryTool.generateAnalysisSql(config);

        assertEquals(
                "SELECT TOP 20 [name], COUNT(*) AS rs_count, ROUND(COUNT(*) * 100.0 / NULLIF((SELECT COUNT(*) FROM [demo].[dbo].[user_account]), 0), 2) AS percentage FROM [demo].[dbo].[user_account] GROUP BY [name] ORDER BY COUNT(*) DESC",
                sql);
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
