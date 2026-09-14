package com.data.pivot.plugin.context;

import com.data.pivot.plugin.config.trigger.DatabaseInfoMapperTrigger;
import com.data.pivot.plugin.config.trigger.DatabaseReferenceMapperTrigger;
import com.data.pivot.plugin.config.trigger.DatabaseUniqueIdMapperTrigger;
import com.data.pivot.plugin.entity.DataPivotDatabaseInfo;
import com.data.pivot.plugin.enums.DBType;
import com.data.pivot.plugin.model.DataPivotCacheContainer;
import com.data.pivot.plugin.tool.DataGripUtil;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.LocalDataSourceManager;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.List;

public class DataPivotDatabaseInfoCacheContainer extends DataPivotCacheContainer<DataPivotDatabaseInfo> {
    public DataPivotDatabaseInfoCacheContainer() {
        super(new DatabaseInfoMapperTrigger(), new DatabaseReferenceMapperTrigger(), new DatabaseUniqueIdMapperTrigger());
    }

    @Override
    protected List<DataPivotDatabaseInfo> init(Project project) {
        LocalDataSourceManager localDataSourceManager = LocalDataSourceManager.getInstance(project);
        List<? extends LocalDataSource> dataSources = localDataSourceManager.getDataSources();
        List<DataPivotDatabaseInfo> dataPivotDatabaseInfos = new ArrayList<>();
        for (LocalDataSource dataSource : dataSources) {
            DBType dbType = DBType.getByName(dataSource.getDbms().getName());
            if (dbType != null && !DBType.MONGO.equals(dbType)) {
                dataPivotDatabaseInfos.addAll(DataGripUtil.loadDatabaseInfo(dataSource));
            }
        }
        return dataPivotDatabaseInfos;
    }
}
