package com.data.pivot.plugin.actions;

import com.data.pivot.plugin.entity.DatabaseQueryConfig;
import com.data.pivot.plugin.model.BaseAnAction;
import com.data.pivot.plugin.tool.DataGripUtil;
import com.data.pivot.plugin.tool.QueryTool;
import com.data.pivot.plugin.view.report.AnalysisResultComponent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DataPivotAnalysisAction extends BaseAnAction {
    @Override
    protected void action(AnActionEvent e) {
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (psiElement == null || editor == null) {
            return;
        }
        DatabaseQueryConfig databaseQueryConfig = DataGripUtil.getDatabaseQueryConfigByPsiElement(psiElement, editor);
        if (databaseQueryConfig == null) {
            return;
        }
        String sql = QueryTool.generateAnalysisSql(databaseQueryConfig);
        databaseQueryConfig.setSql(sql);
        AnalysisResultComponent dialog = new AnalysisResultComponent(e.getProject(), databaseQueryConfig, List.of(), sql);
        dialog.loadResults(QueryTool::query);
        dialog.show();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getData(CommonDataKeys.PSI_ELEMENT) instanceof PsiField);
    }
}
