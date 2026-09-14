package com.data.pivot.plugin.view.setting;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.data.pivot.plugin.config.DataPivotInitializer;
import com.data.pivot.plugin.constants.DataPivotConstants;
import com.data.pivot.plugin.context.DataPivotApplication;
import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.data.pivot.plugin.tool.MessageUtil;
import com.data.pivot.plugin.tool.ProjectUtils;
import com.data.pivot.plugin.view.DataPivotTableColumn;
import com.data.pivot.plugin.view.DataPivotTableView;
import com.data.pivot.plugin.view.ui.DataPivotUi;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;

public class DataPivotMappingSettingView implements Configurable {
    private JPanel mainPanel;
    private DataPivotTableView<DataPivotMappingSettingInfo> tableComponent;
    private List<DataPivotMappingSettingInfo> originalList;
    private JBLabel descriptionLabel;
    private JBLabel helpLabel;

    @Override
    public String getDisplayName() {
        return DataPivotConstants.DATA_PIVOT_MAIN_SETTING;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return getDisplayName();
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (mainPanel == null) {
            initPanel();
        }
        return mainPanel;
    }

    private void initPanel() {
        List<DataPivotMappingSettingInfo> workingCopy = MappingSettingSupport.copyAll(
                DataPivotApplication.getInstance().CACHE.DP_MAPPING_SETTING_INFO_LIST_CACHE.get());
        this.originalList = MappingSettingSupport.copyAll(workingCopy);
        this.tableComponent = new DataPivotTableView<>(
                ListUtil.of(
                        new DataPivotTableColumn<>(DataPivotBundle.message("data.pivot.dialog.setting.module"),
                                DataPivotMappingSettingInfo::getModelName,
                                (data, value) -> data.setModelName((String) value)),
                        new DataPivotTableColumn<>(DataPivotBundle.message("data.pivot.dialog.setting.package"),
                                DataPivotMappingSettingInfo::getPackageName,
                                (data, value) -> data.setPackageName((String) value)),
                        new DataPivotTableColumn<>(DataPivotBundle.message("data.pivot.dialog.setting.database"),
                                DataPivotMappingSettingInfo::getDatabasePath,
                                (data, value) -> data.setDatabasePath((String) value)),
                        new DataPivotTableColumn<>(DataPivotBundle.message("data.pivot.dialog.setting.strategy"),
                                DataPivotMappingSettingInfo::getStrategyCode,
                                (data, value) -> data.setStrategyCode((String) value))
                ),
                workingCopy,
                DataPivotMappingSettingInfoView::new,
                DataPivotMappingSettingInfo.class);
        this.tableComponent.setCheckAddRow((dataList, data) -> {
            if (StrUtil.isEmpty(data.getModelName())) {
                MessageUtil.Dialog.info(DataPivotBundle.message("data.pivot.dialog.setting.module.null"));
                return false;
            }
            if (StrUtil.isEmpty(data.getPackageName())) {
                MessageUtil.Dialog.info(DataPivotBundle.message("data.pivot.dialog.setting.package.null"));
                return false;
            }
            if (StrUtil.isEmpty(data.getDatabasePath())) {
                MessageUtil.Dialog.info(DataPivotBundle.message("data.pivot.dialog.setting.database.null"));
                return false;
            }
            if (StrUtil.isEmpty(data.getStrategyCode())) {
                MessageUtil.Dialog.info(DataPivotBundle.message("data.pivot.dialog.setting.strategy.null"));
                return false;
            }
            String packageReference = data.getPackageReference();
            for (DataPivotMappingSettingInfo existing : dataList) {
                if (existing.getPackageReference() != null && existing.getPackageReference().equals(packageReference)) {
                    MessageUtil.Dialog.info(DataPivotBundle.message("data.pivot.dialog.setting.repeat", packageReference));
                    return false;
                }
            }
            return true;
        });

        AnAction refreshAction = new DumbAwareAction(
                DataPivotBundle.message("data.pivot.view.mapping.setting.refresh.title"),
                DataPivotBundle.message("data.pivot.view.mapping.setting.refresh.description"),
                AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                refreshMetadata();
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };

        descriptionLabel = DataPivotUi.comment(DataPivotBundle.message("data.pivot.view.mapping.setting.description"));
        helpLabel = DataPivotUi.comment(DataPivotBundle.message("data.pivot.view.mapping.setting.help"));
        this.tableComponent.getTable().getEmptyText()
                .setText(DataPivotBundle.message("data.pivot.view.mapping.setting.empty"));
        JComponent tablePanel = this.tableComponent.createPanel(refreshAction);
        this.mainPanel = FormBuilder.createFormBuilder()
                .addComponent(descriptionLabel)
                .addComponentFillVertically(tablePanel, 8)
                .addComponent(helpLabel, 8)
                .getPanel();
        this.mainPanel.setBorder(JBUI.Borders.empty(4, 0));
    }

    void refreshMetadata() {
        DataPivotInitializer.initDataPivotDatabaseInfo(ProjectUtils.getCurrProject());
        DataPivotInitializer.initDataPivotRelation(ProjectUtils.getCurrProject());
        MessageUtil.Dialog.info(DataPivotBundle.message("data.pivot.view.mapping.setting.refresh.content"));
    }

    @Override
    public boolean isModified() {
        if (tableComponent == null) {
            return false;
        }
        return !MappingSettingSupport.same(originalList, tableComponent.getDataList());
    }

    @Override
    public void apply() {
        if (tableComponent == null) {
            return;
        }
        List<DataPivotMappingSettingInfo> current = MappingSettingSupport.copyAll(tableComponent.getDataList());
        new DataPivotMappingSettingInfo().save(current);
        DataPivotApplication.getInstance().CACHE.DP_MAPPING_SETTING_INFO_LIST_CACHE.update(current);
        this.originalList = MappingSettingSupport.copyAll(current);
    }

    @Override
    public void reset() {
        if (tableComponent == null) {
            return;
        }
        List<DataPivotMappingSettingInfo> fromCache = MappingSettingSupport.copyAll(
                DataPivotApplication.getInstance().CACHE.DP_MAPPING_SETTING_INFO_LIST_CACHE.get());
        this.originalList = MappingSettingSupport.copyAll(fromCache);
        this.tableComponent.reload(fromCache);
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
        tableComponent = null;
        originalList = null;
        descriptionLabel = null;
        helpLabel = null;
    }

    DataPivotTableView<DataPivotMappingSettingInfo> getTableComponent() {
        return tableComponent;
    }

    JBLabel getDescriptionLabel() {
        return descriptionLabel;
    }

    JBLabel getHelpLabel() {
        return helpLabel;
    }
}
