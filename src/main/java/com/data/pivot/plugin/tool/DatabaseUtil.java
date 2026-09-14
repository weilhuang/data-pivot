package com.data.pivot.plugin.tool;

import com.data.pivot.plugin.constants.DataPivotConstants;
import com.data.pivot.plugin.context.DataPivotApplication;
import com.data.pivot.plugin.entity.DataPivotDatabaseInfo;
import com.data.pivot.plugin.entity.custom.DataPivotCustomSqlInfo;
import com.data.pivot.plugin.i18n.DataPivotBundle;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库操作工具类
 */
public class DatabaseUtil {

    /**
     * Legacy Statement query against the startup connection map. Unused by Query/Analysis
     * ({@link QueryTool} owns pooled JDBC). Kept so dispose can still drain leftover entries.
     */
    public static List<Map<String, Object>> executeQuery(String querySql,String databaseReference){
        List<Map<String, Object>> resultList = null;
        DataPivotDatabaseInfo dataPivotDatabaseInfo = DataPivotApplication.getInstance().MAPPER.DP_DR_DATABASE_MAPPER.get(databaseReference);
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = DataPivotApplication.getInstance().MAPPER.DR_DATABASE_CONNECTION_MAPPER.get(databaseReference);
            if (connection == null) {
                //连接超时
                MessageUtil.Dialog.info(DataPivotBundle.message("data.pivot.dialog.query.connection.null", dataPivotDatabaseInfo.getDatabasePath()));
                return null;
            }
            statement = connection.createStatement();
            statement.setQueryTimeout(5);
            resultSet = statement.executeQuery(querySql);
            resultList = new ArrayList<>();
            ResultSetMetaData metaData = resultSet.getMetaData();
            while (resultSet.next()) {
                int columnCount = metaData.getColumnCount();
                Map<String, Object> resultMap = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);// 字段名称
                    Object columnValue = resultSet.getObject(columnName);// 字段值
                    resultMap.put(columnName, columnValue);
                }
                resultList.add(resultMap);
            }
        } catch (Exception e){
            //执行超时/sql错误
            MessageUtil.Dialog.info(DataPivotBundle.message("data.pivot.dialog.query.connection.fail", dataPivotDatabaseInfo.getDatabasePath(),e.getMessage(),querySql));
            return null;
        } finally {
            closeResource(connection, statement, resultSet);
        }
        return resultList;
    }

    private static void closeResource(Connection connection,
                                      Statement statement, ResultSet resultSet) {
        try {
            // 注意资源释放顺序
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
//            if (connection != null) {
//                connection.close();
//            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static void closeConnections() {
        Map<String, Connection> databaseConnectionMapper = DataPivotApplication.getInstance().MAPPER.DR_DATABASE_CONNECTION_MAPPER;
        databaseConnectionMapper.forEach((k,v)->{
            try {
                if (v != null) {
                    v.close();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static String createQuerySql(DataPivotCustomSqlInfo dataPivotCustomSqlInfo, String tableName, String columnName) {
        String table = dataPivotCustomSqlInfo.getContent().replace(DataPivotConstants.SQL_TABLE_CODE, tableName);
        String column = table.replace(DataPivotConstants.SQL_COLUMN_CODE, columnName);
        return column;
    }
    public static String buildConnectionString(String url, Map<String, String> properties,String dbName) {
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        sb.append(DataPivotConstants.SLASH +dbName);
        if (!properties.isEmpty()) {
            sb.append("?");
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            // 移除最后一个"&"字符
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }
    public static String buildConnectionString(String url, Map<String, String> properties) {
        StringBuilder sb = new StringBuilder();
        sb.append(url);
        if (!properties.isEmpty()) {
            sb.append("?");
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            // 移除最后一个"&"字符
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }


}
