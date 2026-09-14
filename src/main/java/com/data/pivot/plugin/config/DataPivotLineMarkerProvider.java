package com.data.pivot.plugin.config;

import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.data.pivot.plugin.mapping.MappingConfidence;
import com.data.pivot.plugin.mapping.MappingHit;
import com.data.pivot.plugin.mapping.MappingResolver;
import com.data.pivot.plugin.mapping.NameMatcher;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.database.psi.DbColumn;
import com.intellij.database.psi.DbElement;
import com.intellij.database.psi.DbTable;
import com.intellij.database.view.DbNavigationUtils;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collection;
import java.util.List;

/**
 * Gutter icons for high-confidence table/column mappings only.
 * Query/Analysis reuse {@link MappingResolver}; fuzzy unique-name fallback is not shown in the gutter.
 */
public class DataPivotLineMarkerProvider implements LineMarkerProvider {

    public static void clearCache() {
        MappingResolver.clearCache();
    }

    public static boolean isSimilar(String str1, String str2) {
        return NameMatcher.isSimilar(str1, str2);
    }

    public static double getSimilar(String str1, String str2) {
        return NameMatcher.score(str1, str2);
    }

    public static String preprocess(String str) {
        return NameMatcher.preprocess(str);
    }

    private final Icon AIMING_COLUMN = IconManager.getInstance().getIcon("/icons/aimingColumn.svg", DataPivotLineMarkerProvider.class);
    private final Icon AIMING_TABLE = IconManager.getInstance().getIcon("/icons/aimingTable.svg", DataPivotLineMarkerProvider.class);

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        for (PsiElement element : elements) {
            if (!(element instanceof PsiClass psiClass)) {
                continue;
            }
            MappingHit tableHit = MappingResolver.resolveTable(psiClass);
            if (!tableHit.isHighConfidence() || tableHit.getTable() == null) {
                continue;
            }
            result.add(createLineMarkerInfo(psiClass, tableHit.getTable()));
            for (PsiField psiField : psiClass.getFields()) {
                MappingHit columnHit = MappingResolver.resolveColumn(tableHit, psiField);
                if (columnHit.isHighConfidence() && columnHit.getColumn() != null) {
                    result.add(createLineMarkerInfo(psiField, columnHit.getColumn()));
                }
            }
        }
    }

    public static @Nullable DbTable getTableInfo(PsiClass psiClass) {
        MappingHit hit = MappingResolver.resolveTable(psiClass);
        return hit.isUsableForQuery() ? hit.getTable() : null;
    }

    public static @Nullable MappingHit resolveTableHit(PsiClass psiClass) {
        return MappingResolver.resolveTable(psiClass);
    }

    public static @Nullable DbColumn getColumnInfo(DbTable dbTable, PsiField psiField) {
        if (dbTable == null || psiField == null) {
            return null;
        }
        MappingHit tableHit = MappingResolver.resolveTable(psiField.getContainingClass());
        if (tableHit.getTable() == null) {
            tableHit = new MappingHit(
                    tableHit.getSetting() == null ? MappingConfidence.EXACT : MappingConfidence.NAMING,
                    dbTable,
                    null,
                    tableHit.getSetting());
        }
        MappingHit columnHit = MappingResolver.resolveColumn(tableHit, psiField);
        return columnHit.getColumn() != null && !columnHit.isAmbiguous() ? columnHit.getColumn() : null;
    }

    public static @Nullable DbColumn getColumnInfo(DbTable dbTable, String fieldName) {
        if (dbTable == null) {
            return null;
        }
        MappingHit tableHit = new MappingHit(MappingConfidence.EXACT, dbTable, null, null);
        MappingHit columnHit = MappingResolver.resolveColumn(tableHit, fieldName, null);
        return columnHit.isUsableForQuery() ? columnHit.getColumn() : null;
    }

    private LineMarkerInfo<PsiElement> createLineMarkerInfo(@NotNull PsiElement psiElement, @NotNull DbElement dbElement) {
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
