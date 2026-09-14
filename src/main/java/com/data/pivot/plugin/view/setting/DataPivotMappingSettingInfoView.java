package com.data.pivot.plugin.view.setting;

import cn.hutool.core.util.StrUtil;
import com.data.pivot.plugin.context.DataPivotApplication;
import com.data.pivot.plugin.entity.DataPivotDatabaseInfo;
import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import com.data.pivot.plugin.enums.DefaultStrategyType;
import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.data.pivot.plugin.tool.DataPivotUtil;
import com.data.pivot.plugin.tool.ProjectUtils;
import com.data.pivot.plugin.view.DataPivotTableRowView;
import com.data.pivot.plugin.view.ui.DataPivotUi;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.PsiPackage;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class DataPivotMappingSettingInfoView extends DataPivotTableRowView<DataPivotMappingSettingInfo> {
    private JPanel contentPane;
    private ComboBox<String> moduleComboBox;
    private ComboBox<String> databaseComboBox;
    private ExtendableTextField packageField;
    private ComboBox<String> typeComboBox;
    private JBLabel databaseEmptyHint;
    private final Project project;
    private List<Module> moduleList;
    private List<DataPivotDatabaseInfo> databaseList;
    private List<DefaultStrategyType> typeList;

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.contentPane;
    }

    public DataPivotMappingSettingInfoView() {
        super(ProjectUtils.getCurrProject());
        this.project = ProjectUtils.getCurrProject();
        this.contentPane = buildContentPane();
        this.initPanel();
        setTitle(DataPivotBundle.message("data.pivot.view.mapping.setting.info.title"));
        init();
    }

    private JPanel buildContentPane() {
        this.moduleComboBox = new ComboBox<>();
        this.databaseComboBox = new ComboBox<>();
        this.packageField = new ExtendableTextField();
        this.packageField.addExtension(ExtendableTextComponent.Extension.create(
                AllIcons.Nodes.Package,
                DataPivotBundle.message("data.pivot.view.mapping.setting.info.choose"),
                this::choosePackage));
        this.packageField.getEmptyText().setText(DataPivotBundle.message("data.pivot.view.mapping.setting.info.package"));
        this.typeComboBox = new ComboBox<>();
        this.databaseEmptyHint = DataPivotUi.comment(
                DataPivotBundle.message("data.pivot.view.mapping.setting.info.database.empty"));
        this.databaseEmptyHint.setVisible(false);

        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(DataPivotBundle.message("data.pivot.view.mapping.setting.info.module"), moduleComboBox)
                .addLabeledComponent(DataPivotBundle.message("data.pivot.view.mapping.setting.info.package"), packageField)
                .addLabeledComponent(DataPivotBundle.message("data.pivot.view.mapping.setting.info.database"), databaseComboBox)
                .addComponent(databaseEmptyHint)
                .addLabeledComponent(DataPivotBundle.message("data.pivot.view.mapping.setting.info.strategy"), typeComboBox)
                .getPanel();
        panel.setBorder(JBUI.Borders.empty(8));
        return panel;
    }

    private void choosePackage() {
        PackageChooserDialog dialog = new PackageChooserDialog(
                DataPivotBundle.message("data.pivot.view.mapping.setting.info.package"), project);
        dialog.show();
        PsiPackage psiPackage = dialog.getSelectedPackage();
        if (psiPackage != null) {
            packageField.setText(psiPackage.getQualifiedName());
        }
    }

    public void initDatabaseComponent() {
        this.databaseList = new ArrayList<>(DataPivotApplication.getInstance().CACHE.DP_DB_INFO_LIST_CACHE.get());
        databaseComboBox.removeAllItems();
        for (DataPivotDatabaseInfo dataPivotDatabaseInfo : databaseList) {
            databaseComboBox.addItem(dataPivotDatabaseInfo.getDatabasePath());
        }
        boolean empty = databaseList.isEmpty();
        databaseEmptyHint.setVisible(empty);
        databaseComboBox.setEnabled(!empty);
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (getSelectModule() == null) {
            return new ValidationInfo(DataPivotBundle.message("data.pivot.dialog.setting.module.null"), moduleComboBox);
        }
        if (StrUtil.isEmpty(packageField.getText())) {
            return new ValidationInfo(DataPivotBundle.message("data.pivot.dialog.setting.package.null"), packageField);
        }
        if (getSelectDatabase() == null) {
            return new ValidationInfo(DataPivotBundle.message("data.pivot.dialog.setting.database.null"), databaseComboBox);
        }
        if (getSelectType() == null) {
            return new ValidationInfo(DataPivotBundle.message("data.pivot.dialog.setting.strategy.null"), typeComboBox);
        }
        return null;
    }

    @Override
    public DataPivotMappingSettingInfo getValue() {
        DataPivotMappingSettingInfo dataPivotMappingSettingInfo = new DataPivotMappingSettingInfo();
        Module selectModule = getSelectModule();
        DataPivotDatabaseInfo selectDatabase = getSelectDatabase();
        DefaultStrategyType selectType = getSelectType();
        if (selectModule == null || selectDatabase == null || selectType == null) {
            return dataPivotMappingSettingInfo;
        }
        dataPivotMappingSettingInfo.setModelName(selectModule.getName());
        dataPivotMappingSettingInfo.setPackageName(packageField.getText());
        dataPivotMappingSettingInfo.setDataSourceName(selectDatabase.getDataSourceName());
        dataPivotMappingSettingInfo.setDatabaseName(selectDatabase.getDatabaseName());
        dataPivotMappingSettingInfo.setDatabasePath(selectDatabase.getDatabasePath());
        dataPivotMappingSettingInfo.setStrategyCode(selectType.getCode());
        dataPivotMappingSettingInfo.setDatabaseReference(selectDatabase.getDatabaseReference());
        dataPivotMappingSettingInfo.setPackageReference(
                DataPivotUtil.createPackageReference(selectModule.getName(), packageField.getText()));
        return dataPivotMappingSettingInfo;
    }

    private void initPanel() {
        this.moduleList = new LinkedList<>();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            this.moduleList.add(module);
        }
        for (Module module : this.moduleList) {
            moduleComboBox.addItem(module.getName());
        }
        initDatabaseComponent();
        this.typeList = List.of(DefaultStrategyType.values());
        for (DefaultStrategyType defaultStrategyType : typeList) {
            typeComboBox.addItem(defaultStrategyType.getCode());
        }
    }

    private Module getSelectModule() {
        String name = (String) moduleComboBox.getSelectedItem();
        if (StrUtil.isEmpty(name)) {
            return null;
        }
        return ModuleManager.getInstance(project).findModuleByName(name);
    }

    private DataPivotDatabaseInfo getSelectDatabase() {
        String name = (String) databaseComboBox.getSelectedItem();
        if (StrUtil.isEmpty(name) || databaseList == null) {
            return null;
        }
        List<DataPivotDatabaseInfo> collect = databaseList.stream()
                .filter(bean -> bean.getDatabasePath().equals(name))
                .collect(Collectors.toList());
        if (collect.isEmpty()) {
            return null;
        }
        return collect.get(0);
    }

    private DefaultStrategyType getSelectType() {
        String name = (String) typeComboBox.getSelectedItem();
        if (StrUtil.isEmpty(name) || typeList == null) {
            return null;
        }
        List<DefaultStrategyType> collect = typeList.stream()
                .filter(bean -> bean.getCode().equals(name))
                .collect(Collectors.toList());
        if (collect.isEmpty()) {
            return null;
        }
        return collect.get(0);
    }

    ComboBox<String> getModuleComboBox() {
        return moduleComboBox;
    }

    ComboBox<String> getDatabaseComboBox() {
        return databaseComboBox;
    }

    ExtendableTextField getPackageField() {
        return packageField;
    }

    ComboBox<String> getTypeComboBox() {
        return typeComboBox;
    }

    JBLabel getDatabaseEmptyHint() {
        return databaseEmptyHint;
    }
}
