package com.data.pivot.plugin.actions;

import com.data.pivot.plugin.constants.DataPivotConstants;
import com.data.pivot.plugin.entity.DatabaseQueryConfig;
import com.data.pivot.plugin.model.BaseAnAction;
import com.data.pivot.plugin.tool.DataGripUtil;
import com.data.pivot.plugin.tool.QueryTool;
import com.data.pivot.plugin.view.report.AnalysisResultComponent;
import com.data.pivot.plugin.view.report.AnalysisResultModel;
import com.data.pivot.plugin.view.report.AnalysisRow;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

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
        String sql = DataPivotConstants.DEFAULT_SQL_CONTENT
                .replace(DataPivotConstants.SQL_TABLE_CODE,
                        databaseQueryConfig.getDbName() + "." + databaseQueryConfig.getTableName())
                .replace(DataPivotConstants.SQL_COLUMN_CODE, databaseQueryConfig.getConditionField());
        databaseQueryConfig.setSql(sql);
        List<Map<String, Object>> maps = QueryTool.query(databaseQueryConfig);
        List<AnalysisRow> rows = AnalysisResultModel.fromMaps(maps, databaseQueryConfig.getConditionField());
        new AnalysisResultComponent(e.getProject(), databaseQueryConfig, rows, sql).show();
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
