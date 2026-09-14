package com.data.pivot.plugin.tool;

import cn.hutool.core.util.StrUtil;
import com.data.pivot.plugin.constants.DataPivotConstants;
import com.data.pivot.plugin.entity.DataPivotDatabaseInfo;
import com.data.pivot.plugin.entity.DatabaseQueryConfig;
import com.data.pivot.plugin.enums.DBType;
import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.data.pivot.plugin.mapping.MappingHit;
import com.data.pivot.plugin.mapping.MappingResolver;
import com.intellij.database.cli.DbCliUtil;
import com.intellij.database.dataSource.DatabaseDriver;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.LocalDataSourceManager;
import com.intellij.database.model.ObjectName;
import com.intellij.database.psi.DbColumn;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbElement;
import com.intellij.database.psi.DbTable;
import com.intellij.database.util.TreePattern;
import com.intellij.database.util.TreePatternNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DataGripUtil {
    public static List<DataPivotDatabaseInfo> loadDatabaseInfo(LocalDataSource dataSource) {
        String dataSourceName = dataSource.getName();
        String version = dataSource.getVersion().toString();
        Icon icon = dataSource.getDbms().getIcon();
        String type = dataSource.getDbms().getName();
        List<DataPivotDatabaseInfo> dataPivotDatabaseInfoList = new ArrayList<>();
        String password = DbCliUtil.getPassword((LocalDataSource) dataSource);
        DatabaseDriver databaseDriver = dataSource.getDatabaseDriver();
        String driverClassName = dataSource.getDriverClass();
        List<String> driverClassRootUrls = DataSourceDriverUtil.getDriverClassRootUrls(dataSource);
        String uniqueId = dataSource.getUniqueId();
        String username = dataSource.getUsername();
        Map<String, String> driverProperties = databaseDriver.getDriverProperties();
        for (String dbName : getIntrospectionObjectNames(dataSource)) {
            String jdbcUrl = DatabaseUtil.buildConnectionString(dataSource.getUrl(), driverProperties, dbName);
            DataPivotDatabaseInfo dataPivotDatabaseInfo = new DataPivotDatabaseInfo();
            dataPivotDatabaseInfo.setUniqueId(uniqueId);
            dataPivotDatabaseInfo.setDataSourceVersion(version);
            dataPivotDatabaseInfo.setIcon(icon);
            dataPivotDatabaseInfo.setDataSourceName(dataSourceName);
            dataPivotDatabaseInfo.setDatabaseName(dbName);
            dataPivotDatabaseInfo.setDatabasePath(dataSourceName + DataPivotConstants.SLASH + dbName);
            dataPivotDatabaseInfo.setUserName(username);
            dataPivotDatabaseInfo.setPassword(password);
            dataPivotDatabaseInfo.setDatabaseType(type);
            dataPivotDatabaseInfo.setDatabaseDriver(dataSource.getDatabaseDriver().toString());
            dataPivotDatabaseInfo.setDatabaseParam(driverProperties);
            dataPivotDatabaseInfo.setUrl(jdbcUrl);
            dataPivotDatabaseInfo.setDriverClassName(driverClassName);
            dataPivotDatabaseInfo.setDriverClassRootUrls(driverClassRootUrls);
            dataPivotDatabaseInfo.setDatabaseReference(DataPivotUtil.createDatabaseReference(uniqueId, dbName));
            dataPivotDatabaseInfoList.add(dataPivotDatabaseInfo);
        }
        return dataPivotDatabaseInfoList;
    }

    private static List<String> getIntrospectionObjectNames(LocalDataSource dataSource) {
        List<String> names = new ArrayList<>();
        TreePattern scope = dataSource.getIntrospectionScope();
        if (scope != null && scope.root != null) {
            collectNamingNames(scope.root, names);
        }
        if (!names.isEmpty()) {
            return names;
        }
        dataSource.getModel().getModelRoots().forEach(root -> names.add(root.getName()));
        return names;
    }

    private static void collectNamingNames(TreePatternNode node, List<String> names) {
        if (node == null) {
            return;
        }
        if (node.naming != null && node.naming.names != null) {
            for (ObjectName name : node.naming.names) {
                if (name != null && name.name != null && !name.name.isBlank() && !names.contains(name.name)) {
                    names.add(name.name);
                }
            }
        }
        if (node.groups == null) {
            return;
        }
        for (TreePatternNode.Group group : node.groups) {
            if (group == null || group.children == null) {
                continue;
            }
            for (TreePatternNode child : group.children) {
                if (child != null) {
                    collectNamingNames(child, names);
                }
            }
        }
    }

    public static DatabaseQueryConfig loadDatabaseQueryConfig(LocalDataSource localDataSource, DbTable tableInfo, List<String> allCaretsText, DbColumn columnInfo) {
        String uniqueId = localDataSource.getUniqueId();
        String type = localDataSource.getDbms().getName();

        String url = localDataSource.getUrl();
        DatabaseDriver databaseDriver = localDataSource.getDatabaseDriver();
        Map<String, String> driverProperties = databaseDriver.getDriverProperties();
        String jdbcUrl = DatabaseUtil.buildConnectionString(url, driverProperties);

        String username = localDataSource.getUsername();
        String password = DbCliUtil.getPassword((LocalDataSource) localDataSource);

        //TODO 根据tableInfo 向上找db和schema
        String dbName = null;
        String schema = null;
        DbElement temp = tableInfo;
        while (temp.getParent()!=null){
            DbElement parent = temp.getParent();
            if (StrUtil.endWith(temp.getParent().toString(),"database")) {
                dbName = parent.getName();
            }
            if (StrUtil.endWith(temp.getParent().toString(),"schema")) {
                schema = parent.getName();
            }
            temp = parent;
        }
        if (StrUtil.isEmpty(dbName)){
            dbName = schema;
        }
        return new DatabaseQueryConfig(
                uniqueId,
                DBType.getByName(type),
                jdbcUrl,
                username,
                password,
                localDataSource.getDriverClass(),
                DataSourceDriverUtil.getDriverClassRootUrls(localDataSource),
                dbName,
                schema,
                tableInfo.getName(),
                allCaretsText,
                columnInfo.getName(),
                null,
                null
        );
    }

    public static @Nullable DatabaseQueryConfig getDatabaseQueryConfigByPsiElement(@NotNull PsiElement psiElement,Editor editor) {
        if (!(psiElement instanceof PsiField psiField)) {
            return null;
        }
        MappingHit hit = MappingResolver.resolveField(psiField);
        if (hit.isAmbiguous()) {
            MessageUtil.Hint.error(editor, DataPivotBundle.message(
                    "data.pivot.query.hint.table.ambiguous", psiField.getContainingClass().getName()));
            return null;
        }
        DbTable tableInfo = hit.getTable();
        if (tableInfo == null) {
            MessageUtil.Hint.error(editor, DataPivotBundle.message(
                    "data.pivot.query.hint.table.null", psiField.getContainingClass().getName()));
            return null;
        }
        DbColumn columnInfo = hit.getColumn();
        if (columnInfo == null) {
            MessageUtil.Hint.error(editor, DataPivotBundle.message(
                    "data.pivot.query.hint.column.null", psiField.getName()));
            return null;
        }
        DbDataSource dataSource = tableInfo.getDataSource();

        List<LocalDataSource> dataSources = LocalDataSourceManager.getInstance(psiElement.getProject()).getDataSources();
        LocalDataSource localDataSource = null;
        for (LocalDataSource source : dataSources) {
            String uniqueId = dataSource.getUniqueId();
            if (source.getUniqueId().equals(uniqueId)) {
                localDataSource = source;
                break;
            }
        }
        if (localDataSource == null) {
            MessageUtil.Hint.error(editor, DataPivotBundle.message(
                    "data.pivot.query.hint.datasource.null", psiField.getName()));
            return null;
        }
        DatabaseQueryConfig databaseQueryConfig = DataGripUtil.loadDatabaseQueryConfig(localDataSource, tableInfo, null, columnInfo);
        return databaseQueryConfig;
    }
}
