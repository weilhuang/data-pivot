package com.data.pivot.plugin.view.query;

import com.data.pivot.plugin.DataPivotPlatformTestCase;
import com.data.pivot.plugin.entity.DatabaseQueryConfig;
import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.data.pivot.plugin.view.UiTestFixtures;
import com.intellij.testFramework.JUnit38AssumeSupportRunner;
import org.junit.runner.RunWith;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(JUnit38AssumeSupportRunner.class)
public class QueryTableComponentUiTest extends DataPivotPlatformTestCase {
    public void testQueryDialogShowsRowsStatusAndSearchFields() {
        QueryTableComponent dialog = createDialog(UiTestFixtures.userAccountRows(), config -> List.of());

        assertEquals(2, dialog.getResultTableModel().getRowCount());
        assertEquals(2, dialog.getResultTableModel().getColumnCount());
        assertEquals("id", dialog.getResultTableModel().getColumnName(0));
        assertEquals("alice", dialog.getResultTableModel().getValueAt(0, 1));
        assertEquals(DataPivotBundle.message("data.pivot.query.status.rows", 2), dialog.getStatusLabel().getText());
        assertEquals(DataPivotBundle.message("data.pivot.query.remote.search"),
                dialog.getRemoteSearchField().getTextEditor().getAccessibleContext().getAccessibleName());
        assertEquals(DataPivotBundle.message("data.pivot.query.local.search"),
                dialog.getLocalSearchField().getTextEditor().getAccessibleContext().getAccessibleName());
        assertNotNull(dialog.getHintLabel().getText());
        assertTrue(dialog.getTitle().contains("user_account"));
        assertTrue(dialog.getTitle().contains("name"));
    }

    public void testLocalSearchHighlightsMatchingCellsWithoutQueryingDatabase() {
        AtomicInteger remoteCalls = new AtomicInteger();
        QueryTableComponent dialog = createDialog(UiTestFixtures.userAccountRows(), config -> {
            remoteCalls.incrementAndGet();
            return List.of();
        });

        dialog.performLocalSearch("ali");

        assertEquals(1, dialog.getHighlightedCellCount());
        assertEquals(0, remoteCalls.get());
        dialog.performLocalSearch("");
        assertEquals(0, dialog.getHighlightedCellCount());
    }

    public void testRemoteSearchUsesInjectedRunnerAndUpdatesTable() {
        List<Map<String, Object>> filtered = new ArrayList<>();
        Map<String, Object> bob = new LinkedHashMap<>();
        bob.put("id", 2);
        bob.put("name", "bob");
        filtered.add(bob);

        QueryTableComponent dialog = createDialog(UiTestFixtures.userAccountRows(), config -> {
            if ("bo".equals(config.getLikeValue())) {
                return filtered;
            }
            return UiTestFixtures.userAccountRows();
        });

        dialog.performRemoteSearch("bo");

        assertEquals(1, dialog.getResultTableModel().getRowCount());
        assertEquals("bob", dialog.getResultTableModel().getValueAt(0, 1));
        assertEquals(DataPivotBundle.message("data.pivot.query.status.rows", 1), dialog.getStatusLabel().getText());
    }

    public void testEmptyResultsShowEmptyStateInsteadOfFailing() {
        QueryTableComponent dialog = createDialog(List.of(), config -> List.of());

        assertEquals(0, dialog.getResultTableModel().getRowCount());
        assertEquals(DataPivotBundle.message("data.pivot.query.status.empty"), dialog.getStatusLabel().getText());
        assertEquals(DataPivotBundle.message("data.pivot.query.empty"),
                dialog.getQueryResultTable().getEmptyText().getText());
    }

    public void testCopySelectedRowWritesJsonWithoutModalDialog() {
        QueryTableComponent dialog = createDialog(UiTestFixtures.userAccountRows(), config -> List.of());
        dialog.getQueryResultTable().setRowSelectionInterval(0, 0);

        assertTrue(dialog.copySelectedRow());
        assertTrue(dialog.getLastCopiedText().contains("alice"));
        assertEquals(DataPivotBundle.message("data.pivot.query.status.copied"), dialog.getStatusLabel().getText());
    }

    public void testCopyWithoutSelectionUpdatesStatus() {
        QueryTableComponent dialog = createDialog(UiTestFixtures.userAccountRows(), config -> List.of());
        dialog.getQueryResultTable().clearSelection();

        assertFalse(dialog.copySelectedRow());
        assertEquals(DataPivotBundle.message("data.pivot.query.status.no.selection"), dialog.getStatusLabel().getText());
    }

    public void testPreferredFocusIsRemoteSearch() {
        QueryTableComponent dialog = createDialog(UiTestFixtures.userAccountRows(), config -> List.of());
        JComponent focused = dialog.getPreferredFocusedComponent();
        assertSame(dialog.getRemoteSearchField().getTextEditor(), focused);
    }

    public void testGetInstanceDoesNotQueryUntilRefresh() {
        AtomicInteger remoteCalls = new AtomicInteger();
        DatabaseQueryConfig config = UiTestFixtures.userAccountConfig();
        QueryTableComponent dialog = QueryTableComponent.getInstance(getProject(), config, ignored -> {
            remoteCalls.incrementAndGet();
            return List.of();
        });

        assertEquals(0, remoteCalls.get());
        assertEquals(0, dialog.getResultTableModel().getRowCount());

        dialog.refreshFromDatabase();

        assertEquals(1, remoteCalls.get());
    }

    private QueryTableComponent createDialog(List<Map<String, Object>> rows, QueryRunner runner) {
        DatabaseQueryConfig config = UiTestFixtures.userAccountConfig();
        return new QueryTableComponent(getProject(), config, rows, runner);
    }
}
