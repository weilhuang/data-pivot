package com.data.pivot.plugin.tool;

import com.data.pivot.plugin.entity.DatabaseQueryConfig;
import com.data.pivot.plugin.enums.DBType;
import com.data.pivot.plugin.i18n.DataPivotBundle;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 查询工具类，复用 IntelliJ IDEA Database Tools 中已配置的数据源驱动。
 */
public class QueryTool {

    private static final int LIMIT = 20;
    static final int ANALYSIS_DISTINCT_LIMIT = 20;
    static final String ANALYSIS_COUNT_ALIAS = "rs_count";
    static final String ANALYSIS_PERCENTAGE_ALIAS = "percentage";
    private static final int QUERY_TIMEOUT = 5;
    private static final int MAX_CONNECTIONS = 10;
    private static final int VALIDATION_TIMEOUT = 2;
    private static final ConcurrentMap<String, ConnectionPool> connectionPools = new ConcurrentHashMap<>();

    private static Connection getConnection(DatabaseQueryConfig config) throws InterruptedException, SQLException {
        if (DBType.MONGO.equals(config.getDbType())) {
            throw new SQLException("MongoDB query is not supported without the bundled MongoDB driver. Use Database Tools query files for MongoDB.");
        }

        DataSourceDriverUtil.ensureDriverRegistered(
                config.getDataSourceId(),
                config.getDriverClassName(),
                config.getDriverClassRootUrls()
        );

        String poolKey = config.getDataSourceId() + "@" + config.getUrl();
        ConnectionPool pool = getOrCreateConnectionPool(poolKey, config);
        return pool.acquire();
    }

    private static ConnectionPool getOrCreateConnectionPool(String poolKey, DatabaseQueryConfig config) {
        return connectionPools.computeIfAbsent(poolKey, id -> new ConnectionPool(config));
    }

    private static Connection createConnection(DatabaseQueryConfig config) throws SQLException {
        DBType dbType = config.getDbType();
        if (dbType == null) {
            throw new SQLException("Unsupported database type");
        }

        String url = config.getUrl();
        switch (dbType) {
            case MYSQL:
                url = addUrlParameters(url, "useUnicode=true&characterEncoding=UTF-8");
                break;
            case POSTGRES:
                url = addUrlParameters(url, "charSet=UTF-8");
                break;
            case MSSQL:
                url = url + (url.endsWith(";") ? "" : ";") + "sendStringParametersAsUnicode=true";
                break;
            case ORACLE:
                System.setProperty("oracle.jdbc.defaultNChar", "true");
                break;
            default:
                throw new SQLException("Unsupported DB type: " + dbType);
        }
        return DriverManager.getConnection(url, config.getUser(), config.getPassword());
    }

    static String addUrlParameters(String url, String parameters) {
        if (url.contains("?")) {
            return url + "&" + parameters;
        }
        return url + "?" + parameters;
    }

    private static void releaseConnection(DatabaseQueryConfig config, Connection connection) {
        if (connection == null) {
            return;
        }
        String poolKey = config.getDataSourceId() + "@" + config.getUrl();
        ConnectionPool pool = connectionPools.get(poolKey);
        if (pool != null) {
            pool.release(connection);
        } else {
            ConnectionPool.closeQuietly(connection);
        }
    }

    /**
     * Executes a query. Failures throw {@link QueryFailedException} so callers can show
     * status-bar / notification text instead of a modal dialog (which must not run off the EDT).
     */
    public static @NotNull List<Map<String, Object>> query(DatabaseQueryConfig config) {
        try {
            if (config.isDirectSqlQuery()) {
                return executeSql(config);
            }

            Connection connection = getConnection(config);
            if (connection == null) {
                throw new SQLException("Unable to obtain connection within timeout");
            }
            try {
                String sql = generateSql(config);
                return executeQuery(connection, sql, config.getLikeValue());
            } finally {
                releaseConnection(config, connection);
            }
        } catch (QueryFailedException failed) {
            throw failed;
        } catch (SQLTimeoutException timeoutException) {
            throw new QueryFailedException(
                    DataPivotBundle.message("data.pivot.query.error.timeout", timeoutException.getMessage()),
                    timeoutException);
        } catch (SQLException sqlException) {
            throw new QueryFailedException(
                    DataPivotBundle.message("data.pivot.query.error.sql", sqlException.getMessage()),
                    sqlException);
        } catch (Exception exception) {
            throw new QueryFailedException(
                    DataPivotBundle.message("data.pivot.query.error.connection", exception.getMessage()),
                    exception);
        }
    }

    private static List<Map<String, Object>> executeSql(DatabaseQueryConfig config) throws Exception {
        Connection connection = getConnection(config);
        if (connection == null) {
            throw new SQLException("Unable to obtain connection within timeout");
        }
        try {
            return executeQuery(connection, config.getSql(), null);
        } finally {
            releaseConnection(config, connection);
        }
    }

    static String generateSql(DatabaseQueryConfig config) {
        String columnList = String.join(", ", config.getColumns());
        if (config.getColumns().size() == 1 && config.getColumns().get(0).equals("*")) {
            columnList = "*";
        }

        String conditionClause = "";
        if (config.getLikeValue() != null && !config.getLikeValue().isEmpty()) {
            conditionClause = String.format(" WHERE %s LIKE ?", config.getConditionField());
        }

        String table = unquotedTableRef(config);
        switch (config.getDbType()) {
            case MYSQL:
            case POSTGRES:
                return String.format("SELECT %s FROM %s%s LIMIT %d",
                        columnList, table, conditionClause, LIMIT);
            case ORACLE:
                String oracleLimit = conditionClause.isEmpty() ? " WHERE ROWNUM <= " : " AND ROWNUM <= ";
                return String.format("SELECT %s FROM %s%s%s%d",
                        columnList, table, conditionClause, oracleLimit, LIMIT);
            case MSSQL:
                return String.format("SELECT TOP %d %s FROM %s%s",
                        LIMIT, columnList, table, conditionClause);
            default:
                throw new QueryFailedException("Unsupported DB type: " + config.getDbType());
        }
    }

    /**
     * Value-distribution SQL for Analysis. Limits to {@link #ANALYSIS_DISTINCT_LIMIT} distinct
     * values so the percentage subquery cannot scan unbounded large tables in the UI path.
     */
    public static String generateAnalysisSql(DatabaseQueryConfig config) {
        DBType dbType = config.getDbType();
        if (!DBType.supportsJdbcQuery(dbType)) {
            throw new QueryFailedException("Unsupported DB type: " + dbType);
        }
        String table = quotedTableRef(config);
        String column = quoteIdentifier(dbType, config.getConditionField());
        int limit = ANALYSIS_DISTINCT_LIMIT;
        String selectList = analysisSelectList(dbType, column, table);
        String inner = "SELECT " + selectList + " FROM " + table + " GROUP BY " + column;
        return switch (dbType) {
            case MYSQL, POSTGRES -> inner
                    + " ORDER BY " + analysisAlias(dbType, ANALYSIS_COUNT_ALIAS)
                    + " DESC, " + analysisAlias(dbType, ANALYSIS_PERCENTAGE_ALIAS) + " DESC LIMIT " + limit;
            case ORACLE -> "SELECT * FROM (" + inner + " ORDER BY COUNT(*) DESC) WHERE ROWNUM <= " + limit;
            case MSSQL -> "SELECT TOP " + limit + " " + selectList
                    + " FROM " + table + " GROUP BY " + column
                    + " ORDER BY COUNT(*) DESC";
            default -> throw new QueryFailedException("Unsupported DB type: " + dbType);
        };
    }

    private static String analysisSelectList(DBType dbType, String column, String table) {
        return column
                + ", COUNT(*) AS " + analysisAlias(dbType, ANALYSIS_COUNT_ALIAS)
                + ", ROUND(COUNT(*) * 100.0 / NULLIF((SELECT COUNT(*) FROM " + table + "), 0), 2) AS "
                + analysisAlias(dbType, ANALYSIS_PERCENTAGE_ALIAS);
    }

    static String analysisAlias(DBType dbType, String alias) {
        if (dbType == DBType.ORACLE) {
            return quoteIdentifier(dbType, alias);
        }
        return alias;
    }

    static String unquotedTableRef(DatabaseQueryConfig config) {
        return tableRef(config, false);
    }

    static String quotedTableRef(DatabaseQueryConfig config) {
        return tableRef(config, true);
    }

    static String tableRef(DatabaseQueryConfig config, boolean quoted) {
        DBType dbType = config.getDbType();
        if (dbType == null) {
            String table = config.getTableName();
            return notEmpty(config.getDbName()) ? config.getDbName() + "." + table : table;
        }
        String table = ident(dbType, config.getTableName(), quoted);
        if (dbType == DBType.MSSQL) {
            if (notEmpty(config.getSchema())) {
                return ident(dbType, config.getDbName(), quoted) + "."
                        + ident(dbType, config.getSchema(), quoted) + "."
                        + table;
            }
            return ident(dbType, config.getDbName(), quoted) + ".." + table;
        }
        if (dbType == DBType.POSTGRES || dbType == DBType.ORACLE) {
            if (notEmpty(config.getSchema())) {
                return ident(dbType, config.getSchema(), quoted) + "." + table;
            }
            if (notEmpty(config.getDbName())) {
                return ident(dbType, config.getDbName(), quoted) + "." + table;
            }
            return table;
        }
        if (notEmpty(config.getDbName())) {
            return ident(dbType, config.getDbName(), quoted) + "." + table;
        }
        return table;
    }

    private static String ident(DBType dbType, String identifier, boolean quoted) {
        return quoted ? quoteIdentifier(dbType, identifier) : identifier;
    }

    private static boolean notEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    static String quoteIdentifier(DBType dbType, String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return identifier;
        }
        return switch (dbType) {
            case MYSQL -> "`" + identifier.replace("`", "``") + "`";
            case POSTGRES, ORACLE -> "\"" + identifier.replace("\"", "\"\"") + "\"";
            case MSSQL -> "[" + identifier.replace("]", "]]") + "]";
            default -> identifier;
        };
    }

    private static List<Map<String, Object>> executeQuery(Connection connection, String sql, String likeValue) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            if (likeValue != null && !likeValue.isEmpty()) {
                preparedStatement.setString(1, "%" + likeValue + "%");
            }
            preparedStatement.setQueryTimeout(QUERY_TIMEOUT);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String label = metaData.getColumnLabel(i);
                        if (label == null || label.isEmpty()) {
                            label = metaData.getColumnName(i);
                        }
                        row.put(label, resultSet.getObject(i));
                    }
                    results.add(row);
                }
            }
        }
        return results;
    }

    public static void closeAllConnections() {
        connectionPools.values().forEach(ConnectionPool::closeAll);
        connectionPools.clear();
    }

    /**
     * 懒加载连接池:连接按需创建(上限 {@link #MAX_CONNECTIONS}),取用时通过 {@link Connection#isValid(int)}
     * 校验有效性,失效连接会被丢弃并按需重建。相比一次性预建连接,既避免首次查询因单个连接创建失败而整体抛错,
     * 也避免长时间空闲后使用到已被数据库侧断开的 stale 连接。
     */
    private static final class ConnectionPool {
        private final DatabaseQueryConfig config;
        private final BlockingQueue<Connection> idle = new LinkedBlockingQueue<>(MAX_CONNECTIONS);
        private final AtomicInteger total = new AtomicInteger(0);

        private ConnectionPool(DatabaseQueryConfig config) {
            this.config = config;
        }

        /**
         * 取出一个可用连接;池内无空闲且未达上限时新建,达到上限则等待归还,超时返回 {@code null}。
         */
        private Connection acquire() throws SQLException, InterruptedException {
            long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(QUERY_TIMEOUT);
            while (true) {
                Connection pooled = idle.poll();
                if (pooled != null) {
                    if (isUsable(pooled)) {
                        return pooled;
                    }
                    discard(pooled);
                    continue;
                }
                if (tryReserveSlot()) {
                    try {
                        return createConnection(config);
                    } catch (SQLException e) {
                        total.decrementAndGet();
                        throw e;
                    }
                }
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0) {
                    return null;
                }
                pooled = idle.poll(remainingNanos, TimeUnit.NANOSECONDS);
                if (pooled == null) {
                    return null;
                }
                if (isUsable(pooled)) {
                    return pooled;
                }
                discard(pooled);
            }
        }

        /**
         * 归还连接:已关闭或池已满则直接释放,否则放回空闲队列。
         */
        private void release(Connection connection) {
            if (connection == null) {
                return;
            }
            if (isClosedQuietly(connection) || !idle.offer(connection)) {
                discard(connection);
            }
        }

        private boolean tryReserveSlot() {
            int current;
            do {
                current = total.get();
                if (current >= MAX_CONNECTIONS) {
                    return false;
                }
            } while (!total.compareAndSet(current, current + 1));
            return true;
        }

        private void discard(Connection connection) {
            closeQuietly(connection);
            total.decrementAndGet();
        }

        private void closeAll() {
            Connection connection;
            while ((connection = idle.poll()) != null) {
                closeQuietly(connection);
            }
            total.set(0);
        }

        private static boolean isUsable(Connection connection) {
            try {
                return connection != null && !connection.isClosed() && connection.isValid(VALIDATION_TIMEOUT);
            } catch (SQLException e) {
                return false;
            }
        }

        private static boolean isClosedQuietly(Connection connection) {
            try {
                return connection.isClosed();
            } catch (SQLException e) {
                return true;
            }
        }

        private static void closeQuietly(Connection connection) {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ignored) {
                // Ignore close failures while disposing pooled connections.
            }
        }
    }
}
