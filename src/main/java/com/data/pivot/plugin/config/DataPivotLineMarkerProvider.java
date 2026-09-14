package com.data.pivot.plugin.config;

import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.database.model.DasColumn;
import com.intellij.database.model.DasTable;
import com.intellij.database.psi.DbColumn;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbElement;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.database.psi.DbTable;
import com.intellij.database.util.DasUtil;
import com.intellij.database.util.DbImplUtilCore;
import com.intellij.database.view.DbNavigationUtils;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.ui.IconManager;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * collectSlowLineMarkers 和 getLineMarkerInfo 的主要区别在于它们的性能和用途。
 *
 * getLineMarkerInfo: 这是一个快速的方法，用于为单个 PSI 元素提供行标记信息。它在编辑器滚动和文件变化时被频繁调用，因此需要非常高效。通常用于标记快速决定的行标记。
 *
 * collectSlowLineMarkers: 这是一个用于处理需要较长时间计算的行标记的方法。这个方法在文件解析后被调用，可以用于需要遍历大量 PSI 元素或进行复杂计算的情况。
 *
 * 需要判断字段和类名，然后根据返回的结果添加行标记，这个过程可能涉及较复杂的逻辑或多次调用方法，因此更适合使用 collectSlowLineMarkers。
 */
public class DataPivotLineMarkerProvider implements LineMarkerProvider {

    private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();
    private static final AccessCountCache<PsiClass,DbTable> CACHE = new AccessCountCache<>(5,true);
    private static final AccessCountCache<PsiClass,Boolean> NULL_CACHE = new AccessCountCache<>(3,true);

    public static final void clearCache(){
        CACHE.clear();
        NULL_CACHE.clear();
    }

    public static boolean isSimilar(String str1, String str2) {
        return getSimilar(str1,str2) >= 0.9;
    }

    public static double getSimilar(String str1, String str2) {
        double similarity = JARO_WINKLER.apply(preprocess(str1), preprocess(str2));
        return similarity;
    }

    public static String preprocess(String str) {
        // 将字符串转换为小写并去除下划线
        return str.toLowerCase().replace("_", "");
    }

    private final Icon AIMING_COLUMN = IconManager.getInstance().getIcon("/icons/aimingColumn.svg", DataPivotLineMarkerProvider.class);
    private final Icon AIMING_TABLE = IconManager.getInstance().getIcon("/icons/aimingTable.svg", DataPivotLineMarkerProvider.class);

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        for (PsiElement element : elements) {
            if (element instanceof PsiClass) {
                PsiClass psiClass = (PsiClass) element;
                DbTable dbTable = getTableInfo(psiClass);
                if (dbTable != null) {
                    result.add(createLineMarkerInfo(psiClass,dbTable));
                    for (PsiField psiField : psiClass.getFields()) {
                        DbColumn dbColumn = getColumnInfo(dbTable, psiField);
                        if (dbColumn != null) {
                            result.add(createLineMarkerInfo(psiField,dbColumn));
                        }
                    }
                }

            }
        }
    }

    public static @Nullable DbTable getTableInfo(PsiClass psiClass) {
        if (NULL_CACHE.get(psiClass)!=null) {
            return null;
        }
        DbTable cacheDbTable = CACHE.get(psiClass);
        if (cacheDbTable != null) {
            return cacheDbTable;
        }
        List<DbDataSource> dataSources = DbPsiFacade.getInstance(psiClass.getProject()).getDataSources();
        DasTable maxDasTable  = null;
        DbDataSource maxDbDataSource  = null;
        double maxSimilar  = 0;
        for (DbDataSource dataSource : dataSources) {
            List<? extends DasTable> list = DasUtil.getTables(dataSource).toList();
            for (DasTable dasTable : list) {
                String dasTableName = dasTable.getName();
                String elementName = psiClass.getName();
                double similar = getSimilar(dasTableName, elementName);
                if (similar>=1) {
                    DbTable dbTable = (DbTable) DbImplUtilCore.findElement(dataSource, dasTable);
                    CACHE.put(psiClass,dbTable);
                    return  dbTable;
                }
                if (similar>=0.9 && similar>maxSimilar) {
                    maxSimilar = similar;
                    maxDbDataSource = dataSource;
                    maxDasTable = dasTable;
                }
            }
        }
        if (maxDasTable == null) {
            NULL_CACHE.put(psiClass,true);
            return null;
        } else {
            DbTable dbTable = (DbTable) DbImplUtilCore.findElement(maxDbDataSource, maxDasTable);
            CACHE.put(psiClass,dbTable);
            return dbTable;
        }
    }

    public static @Nullable DbColumn getColumnInfo(DbTable dbTable,PsiField psiField) {
        return getColumnInfo(dbTable, psiField.getName());
    }

    public @Nullable static DbColumn getColumnInfo(DbTable dbTable, String fieldName) {
        DasColumn maxDasColumn  = null;
        double maxSimilar  = 0;
        DbDataSource dataSource = dbTable.getDataSource();
        List<? extends DasColumn> dasColumns = DasUtil.getColumns(dbTable).toList();
        for (DasColumn dasColumn : dasColumns) {
            String columnName = dasColumn.getName();
            double similar = getSimilar(columnName, fieldName);
            if (similar>=1) {
                DbElement dbElement = DbImplUtilCore.findElement(dataSource, dasColumn);
                return (DbColumn) dbElement;
            }
            if (similar>=0.9 && similar>maxSimilar) {
                maxSimilar = similar;
                maxDasColumn = dasColumn;
            }
        }
        if (maxDasColumn == null) {
            return null;
        } else {
            DbElement dbElement = DbImplUtilCore.findElement(dataSource, maxDasColumn);
            return (DbColumn) dbElement;
        }
    }

    private LineMarkerInfo<PsiElement> createLineMarkerInfo(@NotNull PsiElement psiElement, @NotNull DbElement dbElement) {
        // 获取正确的导航元素，确保是字段定义行
        PsiElement navigationElement = psiElement;
        Icon icon = null;
        if (psiElement instanceof PsiField) {
            navigationElement = ((PsiField) psiElement).getNameIdentifier();
            icon = AIMING_COLUMN;
        }
        if (psiElement instanceof PsiClass) {
            navigationElement = ((PsiClass) psiElement).getNameIdentifier();
            icon = AIMING_TABLE;
        }
        return new LineMarkerInfo<>(
                navigationElement,
                navigationElement.getTextRange(),
                icon,
                element -> psiElement instanceof PsiField
                        ? DataPivotBundle.message("data.pivot.gutter.navigate.column")
                        : DataPivotBundle.message("data.pivot.gutter.navigate.table"),
                (e, elt) -> DbNavigationUtils.navigateToDatabaseView(dbElement, true),
                GutterIconRenderer.Alignment.RIGHT,
                () -> DataPivotBundle.message("data.pivot.gutter.accessible")
        );
    }

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }
}

class AccessCountCache<K, V> {
    private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<K, AtomicInteger> accessCountMap = new ConcurrentHashMap<>();
    private final int maxAccessCount;
    private final boolean turnOnCaching;

    public AccessCountCache(int maxAccessCount,boolean turnOnCaching) {
        this.maxAccessCount = maxAccessCount;
        this.turnOnCaching = turnOnCaching;
    }

    public synchronized void clear(){
        cache.clear();
        accessCountMap.clear();
    }

    public V get(K key) {
        if (!turnOnCaching) return null;
        V value = cache.get(key);
        if (value != null) {
            accessCountMap.compute(key, (k, count) -> {
                if (count == null) {
                    count = new AtomicInteger(0);
                }
                if (count.incrementAndGet() >= maxAccessCount) {
                    cache.remove(key);
                    return null;
                }
                return count;
            });
        }
        return value;
    }

    public void put(K key, V value) {
        if (!turnOnCaching) return ;
        cache.put(key, value);
        accessCountMap.put(key, new AtomicInteger(0));
    }
}