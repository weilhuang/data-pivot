package com.data.pivot.plugin.actions;

import com.data.pivot.plugin.config.DataPivotLineMarkerProvider;
import com.data.pivot.plugin.entity.DatabaseQueryConfig;
import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.data.pivot.plugin.tool.DataGripUtil;
import com.data.pivot.plugin.tool.MessageUtil;
import com.data.pivot.plugin.view.query.QueryTableComponent;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.LocalDataSourceManager;
import com.intellij.database.psi.DbColumn;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbTable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Query mapped table rows. The last selected field is the LIKE condition.
 */
public class DataPivotQueryAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getData(CommonDataKeys.PSI_ELEMENT) instanceof PsiField);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (project == null || editor == null || !(psiElement instanceof PsiField)) {
            return;
        }

        PsiField psiField = (PsiField) psiElement;
        PsiClass containingClass = psiField.getContainingClass();
        DbTable tableInfo = DataPivotLineMarkerProvider.getTableInfo(containingClass);
        if (tableInfo == null) {
            MessageUtil.Hint.error(editor, DataPivotBundle.message(
                    "data.pivot.query.hint.table.null", containingClass.getName()));
            return;
        }
        DbColumn columnInfo = DataPivotLineMarkerProvider.getColumnInfo(tableInfo, psiField);
        if (columnInfo == null) {
            MessageUtil.Hint.error(editor, DataPivotBundle.message(
                    "data.pivot.query.hint.column.null", psiField.getName()));
            return;
        }
        List<Caret> allCarets = editor.getCaretModel().getAllCarets();
        List<String> allCaretsText = new ArrayList<>();
        if (allCarets.size() < 2) {
            allCaretsText.add("*");
        } else {
            for (Caret allCaret : allCarets) {
                DbColumn dbColumn = DataPivotLineMarkerProvider.getColumnInfo(tableInfo, allCaret.getSelectedText());
                if (dbColumn != null) {
                    allCaretsText.add(dbColumn.getName());
                }
            }
        }
        DbDataSource dataSource = tableInfo.getDataSource();
        List<LocalDataSource> dataSources = LocalDataSourceManager.getInstance(project).getDataSources();
        LocalDataSource localDataSource = null;
        for (LocalDataSource source : dataSources) {
            if (source.getUniqueId().equals(dataSource.getUniqueId())) {
                localDataSource = source;
                break;
            }
        }
        if (localDataSource == null) {
            MessageUtil.Hint.error(editor, DataPivotBundle.message(
                    "data.pivot.query.hint.datasource.null", psiField.getName()));
            return;
        }
        DatabaseQueryConfig databaseQueryConfig = DataGripUtil.loadDatabaseQueryConfig(
                localDataSource, tableInfo, allCaretsText, columnInfo);
        QueryTableComponent.getInstance(project, databaseQueryConfig).show();
    }
}
