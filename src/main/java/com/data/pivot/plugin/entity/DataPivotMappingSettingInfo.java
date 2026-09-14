package com.data.pivot.plugin.entity;

import com.data.pivot.plugin.entity.custom.DataPivotCustomSqlInfo;
import com.data.pivot.plugin.config.DataPivotDefaultInitializer;
import com.data.pivot.plugin.constants.DataPivotConstants;
import com.data.pivot.plugin.model.DataPivotCache;
import com.data.pivot.plugin.model.DataPivotStorage;
import com.data.pivot.plugin.tool.DataPivotStorageUtil;

import java.util.List;

public class DataPivotMappingSettingInfo implements DataPivotStorage<List<DataPivotMappingSettingInfo>>, DataPivotCache {
    private String modelName;//模块名
    private String packageName;//包路径
    private String dataSourceName;//数据源名
    private String databaseName;//数据库名
    private String databasePath;//数据源名/数据库名
    private String strategyCode;//映射类型:JPA/MP/自定义{表注解(值),字段注解(值)}
    private String databaseReference;
    private String packageReference;
    /**
     * Unused. Analysis SQL is generated per {@link com.data.pivot.plugin.enums.DBType}.
     * Kept so persisted Settings JSON continues to deserialize.
     */
    private String sqlCode = DataPivotConstants.DEFAULT_SQL_CODE;
    /**
     * Unused. See {@link #sqlCode}. Not shown in the Settings form.
     */
    private DataPivotCustomSqlInfo dataPivotCustomSqlInfo = DataPivotDefaultInitializer.getDefaultSql();

    public String getSqlCode() {
        return sqlCode;
    }

    public void setSqlCode(String sqlCode) {
        this.sqlCode = sqlCode;
    }

    public DataPivotCustomSqlInfo getDataPivotCustomSqlInfo() {
        return dataPivotCustomSqlInfo;
    }

    public void setDataPivotCustomSqlInfo(DataPivotCustomSqlInfo dataPivotCustomSqlInfo) {
        this.dataPivotCustomSqlInfo = dataPivotCustomSqlInfo;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabasePath() {
        return databasePath;
    }

    public void setDatabasePath(String databasePath) {
        this.databasePath = databasePath;
    }

    public String getStrategyCode() {
        return strategyCode;
    }

    public void setStrategyCode(String strategyCode) {
        this.strategyCode = strategyCode;
    }

    public String getDatabaseReference() {
        return databaseReference;
    }

    public void setDatabaseReference(String databaseReference) {
        this.databaseReference = databaseReference;
    }

    public String getPackageReference() {
        return packageReference;
    }

    public void setPackageReference(String packageReference) {
        this.packageReference = packageReference;
    }

    @Override
    public List<DataPivotMappingSettingInfo> query() {
        return DataPivotStorageUtil.getListData(DataPivotMappingSettingInfo.class.getName(), DataPivotMappingSettingInfo.class);
    }

    @Override
    public void save(List<DataPivotMappingSettingInfo> dataPivotMappingSettingInfos) {
        DataPivotStorageUtil.setListData(DataPivotMappingSettingInfo.class.getName(), dataPivotMappingSettingInfos);
    }
}
