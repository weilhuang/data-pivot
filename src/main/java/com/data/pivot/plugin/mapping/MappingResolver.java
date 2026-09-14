package com.data.pivot.plugin.mapping;

import cn.hutool.core.util.StrUtil;
import com.data.pivot.plugin.context.DataPivotApplication;
import com.data.pivot.plugin.entity.DataPivotDatabaseInfo;
import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import com.data.pivot.plugin.entity.custom.DataPivotStrategyInfo;
import com.data.pivot.plugin.enums.DBType;
import com.data.pivot.plugin.model.DataPivotStrategyActuator;
import com.data.pivot.plugin.tool.PsiElementUtil;
import com.intellij.database.model.DasColumn;
import com.intellij.database.model.DasObject;
import com.intellij.database.model.DasTable;
import com.intellij.database.psi.DbColumn;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbElement;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.database.psi.DbTable;
import com.intellij.database.util.DasUtil;
import com.intellij.database.util.DbImplUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared resolver for Query, Analysis, and line markers.
 * Prefer Settings + {@link DataPivotStrategyActuator} when a mapping profile exists;
 * otherwise unique exact/fuzzy name match, never a silent max-similarity pick.
 */
public final class MappingResolver {
    private static final StampCache TABLE_CACHE = new StampCache();

    private MappingResolver() {
    }

    public static void clearCache() {
        TABLE_CACHE.clear();
    }

    public static @NotNull MappingHit resolveTable(@Nullable PsiClass psiClass) {
        if (psiClass == null) {
            return MappingHit.unresolved();
        }
        String key = cacheKey(psiClass);
        long stamp = modificationStamp(psiClass);
        MappingHit cached = TABLE_CACHE.getIfFresh(key, stamp);
        if (cached != null) {
            return cached;
        }
        MappingHit resolved = resolveTableUncached(psiClass);
        TABLE_CACHE.put(key, stamp, resolved);
        return resolved;
    }

    public static @NotNull MappingHit resolveField(@Nullable PsiField psiField) {
        if (psiField == null) {
            return MappingHit.unresolved();
        }
        MappingHit tableHit = resolveTable(psiField.getContainingClass());
        if (!tableHit.isUsableForQuery() || tableHit.getTable() == null) {
            return tableHit;
        }
        return resolveColumn(tableHit, psiField);
    }

    public static @NotNull MappingHit resolveColumn(@NotNull MappingHit tableHit, @Nullable PsiField psiField) {
        if (psiField == null || tableHit.getTable() == null) {
            return tableHit.withColumn(null, MappingConfidence.UNRESOLVED);
        }
        return resolveColumn(tableHit, psiField.getName(), psiField);
    }

    public static @NotNull MappingHit resolveColumn(
            @NotNull MappingHit tableHit,
            @Nullable String fieldName,
            @Nullable PsiField psiField
    ) {
        DbTable dbTable = tableHit.getTable();
        if (dbTable == null || StrUtil.isEmpty(fieldName)) {
            return tableHit.withColumn(null, MappingConfidence.UNRESOLVED);
        }
        DataPivotMappingSettingInfo setting = tableHit.getSetting();
        if (setting != null && psiField != null) {
            DataPivotStrategyInfo strategy = DataPivotApplication.getDataPivotStrategyInfo(setting.getStrategyCode());
            String columnName = DataPivotStrategyActuator.resolveColumnName(psiField, strategy);
            DbColumn named = findColumnByName(dbTable, columnName);
            if (named != null) {
                return tableHit.withColumn(named, MappingConfidence.NAMING);
            }
            return tableHit.withColumn(null, MappingConfidence.UNRESOLVED);
        }
        NameMatcher.NameMatch<DasColumn> match = pickColumn(dbTable, fieldName);
        if (match.isAmbiguous()) {
            return MappingHit.ambiguous(setting);
        }
        if (!match.isResolved() || match.value() == null) {
            return tableHit.withColumn(null, MappingConfidence.UNRESOLVED);
        }
        DbElement element = DbImplUtilCore.findElement(dbTable.getDataSource(), match.value());
        return tableHit.withColumn(element instanceof DbColumn ? (DbColumn) element : null, match.confidence());
    }

    public static @Nullable DbColumn findColumnByName(@NotNull DbTable dbTable, @Nullable String columnName) {
        if (StrUtil.isEmpty(columnName)) {
            return null;
        }
        DbDataSource dataSource = dbTable.getDataSource();
        for (DasColumn dasColumn : DasUtil.getColumns(dbTable)) {
            if (columnName.equalsIgnoreCase(dasColumn.getName())) {
                DbElement element = DbImplUtilCore.findElement(dataSource, dasColumn);
                if (element instanceof DbColumn) {
                    return (DbColumn) element;
                }
            }
        }
        return null;
    }

    private static MappingHit resolveTableUncached(@NotNull PsiClass psiClass) {
        Project project = psiClass.getProject();
        DataPivotMappingSettingInfo setting = PsiElementUtil.getMappingSetting(psiClass);
        List<DbDataSource> dataSources = DbPsiFacade.getInstance(project).getDataSources();
        if (setting != null) {
            DataPivotStrategyInfo strategy = DataPivotApplication.getDataPivotStrategyInfo(setting.getStrategyCode());
            String tableName = DataPivotStrategyActuator.resolveTableName(psiClass, strategy);
            DbTable named = findTableByName(dataSources, setting, tableName);
            if (named != null) {
                return new MappingHit(MappingConfidence.NAMING, named, null, setting);
            }
            return new MappingHit(MappingConfidence.UNRESOLVED, null, null, setting);
        }
        return fuzzyTable(psiClass, dataSources);
    }

    private static MappingHit fuzzyTable(@NotNull PsiClass psiClass, @NotNull List<DbDataSource> dataSources) {
        String className = psiClass.getName();
        if (StrUtil.isEmpty(className)) {
            return MappingHit.unresolved();
        }
        List<NameMatcher.Scored<TableCandidate>> scored = new ArrayList<>();
        for (DbDataSource dataSource : dataSources) {
            if (!isJdbcDataSource(dataSource)) {
                continue;
            }
            for (DasTable dasTable : DasUtil.getTables(dataSource)) {
                scored.add(new NameMatcher.Scored<>(
                        new TableCandidate(dataSource, dasTable),
                        NameMatcher.score(dasTable.getName(), className)));
            }
        }
        NameMatcher.NameMatch<TableCandidate> match = NameMatcher.pickUnique(scored);
        if (match.isAmbiguous()) {
            return MappingHit.ambiguous(null);
        }
        if (!match.isResolved() || match.value() == null) {
            return MappingHit.unresolved();
        }
        TableCandidate candidate = match.value();
        DbElement element = DbImplUtilCore.findElement(candidate.dataSource(), candidate.table());
        if (!(element instanceof DbTable)) {
            return MappingHit.unresolved();
        }
        return new MappingHit(match.confidence(), (DbTable) element, null, null);
    }

    private static @Nullable DbTable findTableByName(
            List<DbDataSource> dataSources,
            DataPivotMappingSettingInfo setting,
            @Nullable String tableName
    ) {
        if (StrUtil.isEmpty(tableName)) {
            return null;
        }
        for (DbDataSource dataSource : dataSources) {
            if (!matchesDataSource(dataSource, setting) || !isJdbcDataSource(dataSource)) {
                continue;
            }
            for (DasTable dasTable : DasUtil.getTables(dataSource)) {
                if (!belongsToDatabase(dasTable, setting.getDatabaseName())) {
                    continue;
                }
                if (tableName.equalsIgnoreCase(dasTable.getName())) {
                    DbElement element = DbImplUtilCore.findElement(dataSource, dasTable);
                    if (element instanceof DbTable) {
                        return (DbTable) element;
                    }
                }
            }
        }
        return null;
    }

    private static NameMatcher.NameMatch<DasColumn> pickColumn(DbTable dbTable, String fieldName) {
        List<NameMatcher.Scored<DasColumn>> scored = new ArrayList<>();
        for (DasColumn dasColumn : DasUtil.getColumns(dbTable)) {
            scored.add(new NameMatcher.Scored<>(dasColumn, NameMatcher.score(dasColumn.getName(), fieldName)));
        }
        return NameMatcher.pickUnique(scored);
    }

    static boolean matchesDataSource(@NotNull DbDataSource dataSource, @Nullable DataPivotMappingSettingInfo setting) {
        if (setting == null) {
            return true;
        }
        String uniqueId = settingUniqueId(setting);
        if (StrUtil.isEmpty(uniqueId)) {
            return true;
        }
        return uniqueId.equals(dataSource.getUniqueId());
    }

    static boolean belongsToDatabase(@NotNull DasTable table, @Nullable String databaseName) {
        if (StrUtil.isEmpty(databaseName)) {
            return true;
        }
        DasObject current = table;
        int guard = 0;
        while (current != null && guard++ < 16) {
            if (databaseName.equalsIgnoreCase(current.getName())) {
                return true;
            }
            DasObject parent = current.getDasParent();
            if (parent == null || parent == current) {
                break;
            }
            current = parent;
        }
        return false;
    }

    static @Nullable String settingUniqueId(@Nullable DataPivotMappingSettingInfo setting) {
        if (setting == null) {
            return null;
        }
        String reference = setting.getDatabaseReference();
        if (StrUtil.isNotEmpty(reference)) {
            try {
                DataPivotDatabaseInfo info = DataPivotApplication.getInstance().MAPPER.DP_DR_DATABASE_MAPPER.get(reference);
                if (info != null && StrUtil.isNotEmpty(info.getUniqueId())) {
                    return info.getUniqueId();
                }
            } catch (RuntimeException ignored) {
                // Tests or dispose may lack a current project; fall through to reference parse.
            }
        }
        String databaseName = setting.getDatabaseName();
        if (StrUtil.isNotEmpty(reference) && StrUtil.isNotEmpty(databaseName) && reference.endsWith("." + databaseName)) {
            return reference.substring(0, reference.length() - databaseName.length() - 1);
        }
        return null;
    }

    private static String cacheKey(@NotNull PsiClass psiClass) {
        Project project = psiClass.getProject();
        String qualified = psiClass.getQualifiedName();
        String name = qualified != null ? qualified : String.valueOf(psiClass.getName());
        return project.getLocationHash() + "#" + name;
    }

    static long modificationStamp(@NotNull PsiClass psiClass) {
        PsiFile file = psiClass.getContainingFile();
        return file == null ? Long.MIN_VALUE : file.getModificationStamp();
    }

    static boolean isJdbcDataSource(@NotNull DbDataSource dataSource) {
        try {
            return DBType.supportsJdbcQuery(DBType.getByName(dataSource.getDbms().getName()));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    static final class StampCache {
        private final ConcurrentHashMap<String, Entry> map = new ConcurrentHashMap<>();

        record Entry(MappingHit hit, long stamp) {
        }

        @Nullable MappingHit getIfFresh(@NotNull String key, long stamp) {
            Entry entry = map.get(key);
            if (entry == null || entry.stamp() != stamp) {
                return null;
            }
            return entry.hit();
        }

        void put(@NotNull String key, long stamp, @NotNull MappingHit hit) {
            map.put(key, new Entry(hit, stamp));
        }

        void clear() {
            map.clear();
        }
    }

    private record TableCandidate(DbDataSource dataSource, DasTable table) {
    }
}
