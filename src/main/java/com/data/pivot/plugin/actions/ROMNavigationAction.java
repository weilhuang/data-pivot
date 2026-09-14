package com.data.pivot.plugin.actions;

import com.data.pivot.plugin.context.DataPivotApplication;
import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.data.pivot.plugin.model.BaseAnAction;
import com.data.pivot.plugin.model.DataPivotObject;
import com.data.pivot.plugin.model.DataPivotRelation;
import com.data.pivot.plugin.tool.MessageUtil;
import com.data.pivot.plugin.tool.PsiElementUtil;
import com.intellij.database.datagrid.DataGrid;
import com.intellij.database.datagrid.DataGridUtil;
import com.intellij.database.psi.DbColumn;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiElement;
import com.intellij.util.PsiNavigateUtil;
import org.jetbrains.annotations.NotNull;

public class ROMNavigationAction extends BaseAnAction {
    @Override
    protected void action(AnActionEvent e) {
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (psiElement == null) {
            MessageUtil.mappingError(editor, DataPivotBundle.message("data.pivot.hint.relation.mapping.null", ""));
            return;
        }
        DataPivotRelation dataPivotRelation = PsiElementUtil.getDataPivotRelation(psiElement);
        if (dataPivotRelation.getDataPivotMappingSettingInfo() == null) {
            MessageUtil.mappingError(editor, DataPivotBundle.message(
                    "data.pivot.hint.relation.mapping.null", dataPivotRelation.getDatabaseReference()));
            return;
        }
        DataPivotObject dataPivotObject = DataPivotApplication.romMapping(dataPivotRelation, editor);
        if (dataPivotObject == null) {
            return;
        }
        PsiNavigateUtil.navigate(dataPivotObject.getPsiElement());
    }

    private boolean isDataGrid(AnActionEvent e) {
        DataGrid dataGrid = DataGridUtil.getDataGrid(e.getDataContext());
        return dataGrid != null && dataGrid.getVisibleRows().size() * dataGrid.getVisibleColumns().size() > 0;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        e.getPresentation().setEnabled((psiElement instanceof DbColumn) || isEnabled(e));
    }

    private boolean isEnabled(@NotNull AnActionEvent e) {
        DataGrid dataGrid = DataGridUtil.getDataGrid(e.getDataContext());
        if (dataGrid == null) {
            return false;
        }
        return isDataGrid(e);
    }
}
