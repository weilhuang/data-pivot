package com.data.pivot.plugin.view.setting;

import com.data.pivot.plugin.DataPivotPlatformTestCase;
import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.intellij.testFramework.JUnit38AssumeSupportRunner;
import org.junit.runner.RunWith;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit38AssumeSupportRunner.class)
public class DataPivotSettingsUiTest extends DataPivotPlatformTestCase {
    public void testMappingSettingConfigurableCreatesMainComponent() {
        DataPivotMappingSettingView view = new DataPivotMappingSettingView();

        JComponent component = view.createComponent();

        assertNotNull(component);
        assertEquals("Data-Pivot Configuration", view.getDisplayName());
        assertNotNull(view.getDescriptionLabel());
        assertEquals(DataPivotBundle.message("data.pivot.view.mapping.setting.description"),
                view.getDescriptionLabel().getText());
        assertEquals(DataPivotBundle.message("data.pivot.view.mapping.setting.help"),
                view.getHelpLabel().getText());
        assertFalse(view.isModified());
        assertEquals(DataPivotBundle.message("data.pivot.view.mapping.setting.empty"),
                view.getTableComponent().getTable().getEmptyText().getText());
    }

    public void testSettingsBecomeModifiedAfterInlineEditAndResetClearsFlag() {
        DataPivotMappingSettingView view = new DataPivotMappingSettingView();
        view.createComponent();
        DataPivotMappingSettingInfo row = new DataPivotMappingSettingInfo();
        row.setModelName("app");
        row.setPackageName("com.example");
        row.setDatabasePath("ds/demo");
        row.setStrategyCode("JPA");
        row.setPackageReference("app/com.example");
        view.getTableComponent().getDataList().add(row);

        assertTrue("adding a working copy row should mark the configurable modified", view.isModified());

        view.reset();
        assertFalse(view.isModified());
    }

    public void testMappingSettingInfoDialogCreatesLabeledForm() {
        DataPivotMappingSettingInfoView view = new DataPivotMappingSettingInfoView();

        JComponent component = view.createCenterPanel();

        assertNotNull(component);
        assertTrue(component.getComponentCount() > 0);
        assertNotNull(view.getModuleComboBox());
        assertNotNull(view.getPackageField());
        assertNotNull(view.getDatabaseComboBox());
        assertNotNull(view.getTypeComboBox());
        assertEquals(3, view.getTypeComboBox().getItemCount());
        assertTrue(String.join(" ", findLabelText(component)).contains(
                DataPivotBundle.message("data.pivot.view.mapping.setting.info.module")));
        assertTrue(view.getDatabaseEmptyHint().isVisible());
        assertFalse(view.getDatabaseComboBox().isEnabled());
    }

    public void testCreateComponentIsIdempotent() {
        DataPivotMappingSettingView view = new DataPivotMappingSettingView();
        JComponent first = view.createComponent();
        JComponent second = view.createComponent();
        assertSame(first, second);
    }

    private static List<String> findLabelText(Component root) {
        List<String> labels = new ArrayList<>();
        collectLabels(root, labels);
        return labels;
    }

    private static void collectLabels(Component component, List<String> labels) {
        if (component instanceof JLabel) {
            labels.add(((JLabel) component).getText());
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                collectLabels(child, labels);
            }
        }
    }
}
