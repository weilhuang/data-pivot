package com.data.pivot.plugin.context;

import com.data.pivot.plugin.entity.DataPivotDatabaseInfo;
import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import com.data.pivot.plugin.entity.custom.DataPivotCustomSqlInfo;
import com.data.pivot.plugin.entity.custom.DataPivotStrategyInfo;
import com.data.pivot.plugin.model.DataPivotCacheContainer;
import com.data.pivot.plugin.model.DataPivotObject;
import com.data.pivot.plugin.model.DataPivotRelation;
import com.data.pivot.plugin.model.DataPivotStrategyActuator;
import com.data.pivot.plugin.tool.ProjectUtils;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataPivotApplication {
    private final Project project;
    public final Cache CACHE;
    public final Mapper MAPPER;

    public DataPivotApplication(Project project) {
        this.project = project;
        this.CACHE = new Cache();
        this.MAPPER = new Mapper();
    }
    public static DataPivotApplication getInstance(){
        return getInstance(ProjectUtils.getCurrProject());
    }

    /**
     * Project-scoped service lookup. Mapping, gutter, and Query/Analysis must pass the
     * {@code PsiElement}'s project rather than relying on the focused window.
     * {@code null} falls back to {@link ProjectUtils#getCurrProject()} (Settings UI).
     */
    public static DataPivotApplication getInstance(Project project){
        Project resolved = project != null ? project : ProjectUtils.getCurrProject();
        return ServiceManager.getService(resolved, DataPivotApplication.class);
    }

    public Project getProject() {
        return project;
    }
    public class Cache{
        public final DataPivotCacheContainer<DataPivotDatabaseInfo> DP_DB_INFO_LIST_CACHE = new DataPivotDatabaseInfoCacheContainer();
        public final DataPivotCacheContainer<DataPivotMappingSettingInfo> DP_MAPPING_SETTING_INFO_LIST_CACHE = new DataPivotMappingSettingInfoCacheContainer();
        public final DataPivotCacheContainer<DataPivotRelation> DP_RELATION_INFO_LIST_CACHE = new DataPivotRelationCacheContainer();

    }
    public class Mapper{
        public final Map<String, DataPivotStrategyInfo> DEFAULT_STRATEGY_MAPPER = new ConcurrentHashMap<>();
        public final Map<String, DataPivotCustomSqlInfo> DEFAULT_SQL_MAPPER = new ConcurrentHashMap<>();
        public final Map<String, Connection> DR_DATABASE_CONNECTION_MAPPER = new ConcurrentHashMap<>();
        public final Map<String,List<String>> DP_DS_DATABASE_MAPPER = new ConcurrentHashMap<>();
        public final Map<String, DataPivotDatabaseInfo> DP_DP_DATABASE_MAPPER = new ConcurrentHashMap<>();
        public final Map<String, DataPivotDatabaseInfo> DP_DR_DATABASE_MAPPER = new ConcurrentHashMap<>();
        public final Map<String,Map<String, DataPivotMappingSettingInfo>> DP_ORM_SETTING_MAPPER = new ConcurrentHashMap<>();
        public final Map<String,Map<String, DataPivotMappingSettingInfo>> DP_ROM_SETTING_MAPPER = new ConcurrentHashMap<>();
        public final Map<String, Map<String, Map<String, DataPivotRelation>>> DP_RELATION_INFO_MAPPER = new ConcurrentHashMap<>();
    }

    public static DataPivotObject romMapping(DataPivotRelation dataPivotRelation, Editor editor){
        return DataPivotStrategyActuator.mapping(dataPivotRelation,editor);
    }
    public static DataPivotRelation ormMapping(DataPivotObject dataPivotObject, Editor editor){
        return DataPivotStrategyActuator.mapping(dataPivotObject,editor);
    }

    public static DataPivotStrategyInfo getDataPivotStrategyInfo(String strategyCode) {
        return getDataPivotStrategyInfo(ProjectUtils.getCurrProject(), strategyCode);
    }

    public static DataPivotStrategyInfo getDataPivotStrategyInfo(Project project, String strategyCode) {
        if (strategyCode == null) {
            return null;
        }
        return getInstance(project).MAPPER.DEFAULT_STRATEGY_MAPPER.get(strategyCode);
    }


}
