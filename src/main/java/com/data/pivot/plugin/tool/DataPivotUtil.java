package com.data.pivot.plugin.tool;

import cn.hutool.core.util.StrUtil;
import com.data.pivot.plugin.constants.DataPivotConstants;
import com.data.pivot.plugin.context.DataPivotApplication;
import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 匹配优先级:
 * 同路径,取先后,
 * 不同路径取路径前缀短
 */
public class DataPivotUtil {
    /**
     * 1.seq定义:
     * xxxReference,xxx指所到层级
     *
     * 2.Relation层级:
     * uid.ds.db.namespace.table.column
     *
     * 3.Object层级:
     * module/package.class#feild
     */
    public static String createPackageReference(String moduleName, String packageName) {
        return moduleName+ DataPivotConstants.SLASH+packageName;
    }
    public static String createClassReference(String moduleName, String packageName,String className) {
        return createPackageReference(moduleName,packageName)+DataPivotConstants.DOT+className;
    }
    public static String createFieldReference(String moduleName, String packageName,String className,String fieldName) {
        return createClassReference(moduleName,packageName,className)+DataPivotConstants.NUMBER_SIGN+fieldName;
    }
    public static String createDatabaseReference(String uniqueId, String databaseName) {
        return uniqueId+ DataPivotConstants.DOT+databaseName;
    }
    public static String createTableReference(String uniqueId, String databaseName,String tableName) {
        return createDatabaseReference(uniqueId,databaseName)+DataPivotConstants.DOT+tableName;
    }
    public static String createColumnReference(String uniqueId, String databaseName,String tableName,String columnName) {
        return createTableReference(uniqueId,databaseName,tableName)+DataPivotConstants.DOT+columnName;
    }
    public static String createColumnPath(String databaseName,String tableName,String columnName) {
        return databaseName+DataPivotConstants.DOT+tableName+DataPivotConstants.DOT+columnName;
    }
    public static boolean compareReference(String base, String value){
        return StrUtil.startWith(value,base);
    }
    public static boolean comparePackageName(String base,String value){
        return StrUtil.startWith(value,base);
    }

    public static @Nullable DataPivotMappingSettingInfo getObjectDataPivotSettingInfo(String packageReference) {
        return getObjectDataPivotSettingInfo(null, packageReference);
    }

    public static @Nullable DataPivotMappingSettingInfo getObjectDataPivotSettingInfo(
            @Nullable Project project, String packageReference) {
        List<DataPivotMappingSettingInfo> dpSettingInfoListCache =
                DataPivotApplication.getInstance(project).CACHE.DP_MAPPING_SETTING_INFO_LIST_CACHE.get();
        Map<String, DataPivotMappingSettingInfo> rs = new HashMap<>();
        for (DataPivotMappingSettingInfo dataPivotMappingSettingInfo : dpSettingInfoListCache) {
            if(compareReference(dataPivotMappingSettingInfo.getPackageReference(),packageReference)){
                rs.put(dataPivotMappingSettingInfo.getPackageReference(), dataPivotMappingSettingInfo);
            }
        }
        AtomicInteger index = new AtomicInteger();
        AtomicReference<DataPivotMappingSettingInfo> dataPivotSettingInfo=new AtomicReference<>();
        rs.forEach((k,v)->{
            if (k.length()>= index.get()) {
                dataPivotSettingInfo.set(v);
                index.set(k.length());
            }
        });
        return dataPivotSettingInfo.get();
    }
    public static @Nullable DataPivotMappingSettingInfo getRelationDataPivotSettingInfo(String databaseReference) {
        return getRelationDataPivotSettingInfo(null, databaseReference);
    }

    public static @Nullable DataPivotMappingSettingInfo getRelationDataPivotSettingInfo(
            @Nullable Project project, String databaseReference) {
        List<DataPivotMappingSettingInfo> dpSettingInfoListCache =
                DataPivotApplication.getInstance(project).CACHE.DP_MAPPING_SETTING_INFO_LIST_CACHE.get();
        Map<String, DataPivotMappingSettingInfo> rs = new HashMap<>();
        for (DataPivotMappingSettingInfo dataPivotMappingSettingInfo : dpSettingInfoListCache) {
            if(dataPivotMappingSettingInfo.getDatabaseReference().equals(databaseReference)){
                rs.put(dataPivotMappingSettingInfo.getPackageReference(), dataPivotMappingSettingInfo);
            }
        }
        //取最长,即粒度最低
        AtomicInteger index = new AtomicInteger();
        AtomicReference<DataPivotMappingSettingInfo> dataPivotSettingInfo=new AtomicReference<>();
        rs.forEach((k,v)->{
            if (k.length()>= index.get()) {
                dataPivotSettingInfo.set(v);
                index.set(k.length());
            }
        });
        return dataPivotSettingInfo.get();
    }
}
