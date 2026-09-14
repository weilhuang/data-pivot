package com.data.pivot.plugin.config;


import com.data.pivot.plugin.entity.custom.DataPivotStrategyInfo;
import com.data.pivot.plugin.tool.DataSourceDriverUtil;
import com.data.pivot.plugin.tool.DatabaseUtil;
import com.data.pivot.plugin.tool.QueryTool;
import com.data.pivot.plugin.context.DataPivotApplication;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;

public class DataPivotInitializer implements StartupActivity.DumbAware {

    @Override
    public void runActivity(Project project) {
        initDefaultStrategy(DataPivotApplication.getInstance(project));
        initDataPivotDatabaseInfo(project);
        initDataPivotSettingInfo(project);
        // Safety net for any leftover entries; Query uses QueryTool pools, not startup JDBC.
        Disposer.register(project, DatabaseUtil::closeConnections);
        Disposer.register(project, QueryTool::closeAllConnections);
        Disposer.register(project, DataSourceDriverUtil::deregisterAllDrivers);
    }

    static void initDefaultStrategy(DataPivotApplication application) {
        DataPivotStrategyInfo jpaStrategy = DataPivotDefaultInitializer.getJPAStrategy();
        DataPivotStrategyInfo mpStrategy = DataPivotDefaultInitializer.getMPStrategy();
        DataPivotStrategyInfo huStrategy = DataPivotDefaultInitializer.getHUStrategy();
        application.MAPPER.DEFAULT_STRATEGY_MAPPER.put(jpaStrategy.getCode(), jpaStrategy);
        application.MAPPER.DEFAULT_STRATEGY_MAPPER.put(mpStrategy.getCode(), mpStrategy);
        application.MAPPER.DEFAULT_STRATEGY_MAPPER.put(huStrategy.getCode(), huStrategy);
    }

    static public void initDataPivotDatabaseInfo(Project project) {
        DataPivotApplication.getInstance(project).CACHE.DP_DB_INFO_LIST_CACHE.loading(project);
    }
    private void initDataPivotSettingInfo(Project project) {
        DataPivotApplication.getInstance(project).CACHE.DP_MAPPING_SETTING_INFO_LIST_CACHE.loading(project);
    }

    public static boolean isInitDataPivotRelation() {
        return !DataPivotApplication.getInstance().CACHE.DP_RELATION_INFO_LIST_CACHE.isEmpty();
    }

    public static void initDataPivotRelation(Project project) {
        DataPivotApplication.getInstance(project).CACHE.DP_RELATION_INFO_LIST_CACHE.loading(project);
    }

}
