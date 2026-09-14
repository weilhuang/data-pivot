package com.data.pivot.plugin.view;

import com.data.pivot.plugin.DataPivotPlatformTestCase;
import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.data.pivot.plugin.view.setting.DataPivotMappingSettingInfoView;
import com.intellij.testFramework.JUnit38AssumeSupportRunner;
import org.junit.runner.RunWith;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit38AssumeSupportRunner.class)
public class DataPivotTableViewUiTest extends DataPivotPlatformTestCase {
    public void testTableRendersConfiguredColumnsAndRows() {
        DataPivotMappingSettingInfo info = new DataPivotMappingSettingInfo();
        info.setModelName("app");
        info.setPackageName("com.example");
        DataPivotTableView<DataPivotMappingSettingInfo> table = newTable(new ArrayList<>(List.of(info)));

        assertEquals(1, table.getRowCount());
        assertEquals(2, table.getColumnCount());
        assertEquals("app", table.getValueAt(0, 0));
        assertEquals("com.example", table.getValueAt(0, 1));
        JComponent panel = table.createPanel();
        assertNotNull(panel);
        assertEquals(DataPivotBundle.message("data.pivot.table.empty"), table.getTable().getEmptyText().getText());
        assertTrue(table.getTable().getRowSorter() != null);
    }

    public void testReloadReplacesRows() {
        DataPivotMappingSettingInfo first = new DataPivotMappingSettingInfo();
        first.setModelName("one");
        DataPivotTableView<DataPivotMappingSettingInfo> table = newTable(new ArrayList<>(List.of(first)));

        DataPivotMappingSettingInfo second = new DataPivotMappingSettingInfo();
        second.setModelName("two");
        table.reload(new ArrayList<>(List.of(second)));

        assertEquals(1, table.getRowCount());
        assertEquals("two", table.getValueAt(0, 0));
        assertEquals(1, table.getDataList().size());
    }

    public void testEmptyTableHasNoRows() {
        DataPivotTableView<DataPivotMappingSettingInfo> table = newTable(new ArrayList<>());
        assertEquals(0, table.getRowCount());
        assertTrue(table.getDataList().isEmpty());
    }

    private static DataPivotTableView<DataPivotMappingSettingInfo> newTable(List<DataPivotMappingSettingInfo> data) {
        return new DataPivotTableView<>(
                List.of(
                        new DataPivotTableColumn<>("module", DataPivotMappingSettingInfo::getModelName),
                        new DataPivotTableColumn<>("package", DataPivotMappingSettingInfo::getPackageName)
                ),
                data,
                DataPivotMappingSettingInfoView::new,
                DataPivotMappingSettingInfo.class
        );
    }
}
