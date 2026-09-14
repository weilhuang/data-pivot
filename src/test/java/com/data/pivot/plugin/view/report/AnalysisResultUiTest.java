package com.data.pivot.plugin.view.report;

import com.data.pivot.plugin.DataPivotPlatformTestCase;
import com.data.pivot.plugin.entity.DatabaseQueryConfig;
import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.data.pivot.plugin.view.UiTestFixtures;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.testFramework.JUnit38AssumeSupportRunner;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(JUnit38AssumeSupportRunner.class)
public class AnalysisResultUiTest extends DataPivotPlatformTestCase {
    public void testAnalysisDialogShowsDistributionTableAndSql() {
        AnalysisResultComponent dialog = createDialog(AnalysisResultModel.fromMaps(
                UiTestFixtures.analysisRows(), "name"));

        assertEquals(2, dialog.getResultTable().getRowCount());
        assertEquals(3, dialog.getResultTable().getColumnCount());
        assertEquals(DataPivotBundle.message("data.pivot.analysis.column.value"),
                dialog.getResultTable().getColumnName(0));
        assertEquals("ACTIVE", dialog.getResultTable().getValueAt(0, 0));
        assertEquals("18", dialog.getResultTable().getValueAt(0, 1));
        assertEquals(DataPivotBundle.message("data.pivot.analysis.status.rows", 2), dialog.getStatusLabel().getText());
        assertEquals(sampleSql(), dialog.getSqlArea().getText());
        assertFalse(dialog.getSqlArea().isEditable());
        assertTrue(dialog.getTitle().contains("user_account"));
        assertNotNull(dialog.getHintLabel().getText());
    }

    public void testEmptyAnalysisShowsEmptyState() {
        AnalysisResultComponent dialog = createDialog(List.of());

        assertEquals(0, dialog.getResultTable().getRowCount());
        assertEquals(DataPivotBundle.message("data.pivot.analysis.empty"), dialog.getStatusLabel().getText());
        assertEquals(DataPivotBundle.message("data.pivot.analysis.empty"),
                dialog.getResultTable().getEmptyText().getText());
    }

    public void testCopySqlAndSelectedValue() {
        AnalysisResultComponent dialog = createDialog(AnalysisResultModel.fromMaps(
                UiTestFixtures.analysisRows(), "name"));

        assertTrue(dialog.copySql());
        assertEquals(sampleSql(), dialog.getLastCopiedText());
        assertEquals(DataPivotBundle.message("data.pivot.analysis.copied.sql"), dialog.getStatusLabel().getText());

        dialog.getResultTable().setRowSelectionInterval(1, 1);
        assertTrue(dialog.copySelectedValue());
        assertEquals("DISABLED", dialog.getLastCopiedText());
        assertEquals(DataPivotBundle.message("data.pivot.analysis.copied.value"), dialog.getStatusLabel().getText());
    }

    public void testLookupElementPresentsValueAndDoesNotUseSqlAsInsertText() {
        AnalysisRow row = new AnalysisRow("ACTIVE", "18", "90.0");
        DataPivotLookupElement element = AnalysisResultModel.toLookupElement(row, "status", sampleSql());
        LookupElementPresentation presentation = new LookupElementPresentation();

        element.renderElement(presentation);

        assertEquals("status", element.getLookupString());
        assertEquals("ACTIVE", presentation.getItemText());
        assertEquals(AnalysisResultModel.formatInfo(row), presentation.getTypeText());
        assertEquals(sampleSql(), element.getSql());
        assertEquals("ACTIVE", element.getData());
    }

    public void testLoadResultsUsesInjectedRunnerWithoutLiveJdbc() {
        AnalysisResultComponent dialog = createDialog(List.of());
        dialog.loadResults(config -> UiTestFixtures.analysisRows());

        assertEquals(2, dialog.getResultTable().getRowCount());
        assertEquals("ACTIVE", dialog.getResultTable().getValueAt(0, 0));
        assertEquals(DataPivotBundle.message("data.pivot.analysis.status.rows", 2), dialog.getStatusLabel().getText());
    }

    private AnalysisResultComponent createDialog(List<AnalysisRow> rows) {
        DatabaseQueryConfig config = UiTestFixtures.userAccountConfig();
        return new AnalysisResultComponent(getProject(), config, rows, sampleSql());
    }

    private static String sampleSql() {
        return "SELECT name, COUNT(*) AS rs_count FROM demo.user_account GROUP BY name";
    }
}
