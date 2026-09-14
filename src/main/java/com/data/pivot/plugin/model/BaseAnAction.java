package com.data.pivot.plugin.model;

import com.data.pivot.plugin.config.DataPivotInitializer;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public abstract class BaseAnAction extends AnAction {
    protected Editor editor;
    protected Project project;
    protected PsiElement psiElement;

    @Override
    final public void actionPerformed(@NotNull AnActionEvent e) {
        editor = e.getData(CommonDataKeys.EDITOR);
        project = e.getProject();
        psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        loadDataGridInfo(e.getProject());
        action(e);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    protected abstract void action(AnActionEvent e);

    private void loadDataGridInfo(Project project) {
        if (!DataPivotInitializer.isInitDataPivotRelation()) {
            DataPivotInitializer.initDataPivotRelation(project);
        }
    }
}
