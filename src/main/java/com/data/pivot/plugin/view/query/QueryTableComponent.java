package com.data.pivot.plugin.view.query;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.data.pivot.plugin.entity.DatabaseQueryConfig;
import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.data.pivot.plugin.tool.BackgroundQuerySupport;
import com.data.pivot.plugin.tool.MessageUtil;
import com.data.pivot.plugin.tool.QueryTool;
import com.data.pivot.plugin.view.ui.DataPivotUi;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.Alarm;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class QueryTableComponent extends DialogWrapper {
    private static final int SEARCH_DEBOUNCE_MS = 300;

    private final DatabaseQueryConfig databaseQueryConfig;
    private final QueryRunner queryRunner;
    private final Project project;
    private final Alarm remoteSearchAlarm;
    private final Alarm localSearchAlarm;
    private final AtomicInteger queryGeneration = new AtomicInteger();

    private JBTable queryResultTable;
    private ResultTableModel resultTableModel;
    private SearchTextField localSearchField;
    private SearchTextField remoteSearchField;
    private JBLabel statusLabel;
    private JBLabel hintLabel;
    private final List<Pair<Integer, Integer>> highlightedCells = new ArrayList<>();
    private String lastCopiedText = "";

    public QueryTableComponent(@Nullable Project project,
                               @NotNull DatabaseQueryConfig databaseQueryConfig,
                               @NotNull List<Map<String, Object>> queryResults) {
        this(project, databaseQueryConfig, queryResults, QueryTool::query);
    }

    public QueryTableComponent(@Nullable Project project,
                               @NotNull DatabaseQueryConfig databaseQueryConfig,
                               @NotNull List<Map<String, Object>> queryResults,
                               @NotNull QueryRunner queryRunner) {
        super(project, false);
        this.project = project;
        this.databaseQueryConfig = databaseQueryConfig;
        this.queryRunner = queryRunner;
        this.remoteSearchAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, getDisposable());
        this.localSearchAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, getDisposable());
        initModel(queryResults);
        setTitle(DataPivotBundle.message(
                "data.pivot.query.title.detail",
                nullToEmpty(databaseQueryConfig.getTableName()),
                nullToEmpty(databaseQueryConfig.getConditionField())));
        setModal(false);
        setOKButtonText(DataPivotBundle.message("data.pivot.dialog.close"));
        init();
        setSize(JBUI.scale(900), JBUI.scale(520));
    }

    public static @NotNull QueryTableComponent getInstance(@Nullable Project project,
                                                           @NotNull DatabaseQueryConfig databaseQueryConfig) {
        return getInstance(project, databaseQueryConfig, QueryTool::query);
    }

    public static @NotNull QueryTableComponent getInstance(@Nullable Project project,
                                                           @NotNull DatabaseQueryConfig databaseQueryConfig,
                                                           @NotNull QueryRunner queryRunner) {
        return new QueryTableComponent(project, databaseQueryConfig, List.of(), queryRunner);
    }

    public void refreshFromDatabase() {
        updateTable(remoteSearchField == null ? databaseQueryConfig.getLikeValue() : remoteSearchField.getText());
    }

    private void initModel(@NotNull List<Map<String, Object>> queryResults) {
        String[] columnNames = resolveColumnNames(queryResults);
        List<QueryTableRow> rows = toRows(queryResults, columnNames);
        this.resultTableModel = new ResultTableModel(rows, columnNames);
    }

    @Override
    protected @NotNull JComponent createCenterPanel() {
        queryResultTable = new JBTable(resultTableModel);
        DataPivotUi.configureTable(queryResultTable, DataPivotBundle.message("data.pivot.query.empty"));
        queryResultTable.setDefaultRenderer(Object.class, new HighlightRenderer());
        DataPivotUi.applyFixedColumnWidths(queryResultTable);
        queryResultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    copySelectedRow();
                }
            }
        });

        remoteSearchField = DataPivotUi.searchField(
                DataPivotBundle.message("data.pivot.query.remote.search"),
                DataPivotBundle.message("data.pivot.query.remote.search.placeholder"),
                DataPivotBundle.message("data.pivot.query.remote.search.tooltip"));
        localSearchField = DataPivotUi.searchField(
                DataPivotBundle.message("data.pivot.query.local.search"),
                DataPivotBundle.message("data.pivot.query.local.search.placeholder"),
                DataPivotBundle.message("data.pivot.query.local.search.tooltip"));

        localSearchField.getTextEditor().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                scheduleLocalSearch();
            }
        });
        remoteSearchField.getTextEditor().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                scheduleRemoteSearch();
            }
        });

        statusLabel = DataPivotUi.status(statusText());
        hintLabel = DataPivotUi.comment(DataPivotBundle.message("data.pivot.query.hint"));

        JPanel searchPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(DataPivotBundle.message("data.pivot.query.remote.search"), remoteSearchField)
                .addLabeledComponent(DataPivotBundle.message("data.pivot.query.local.search"), localSearchField)
                .getPanel();

        JPanel panel = FormBuilder.createFormBuilder()
                .addComponent(searchPanel)
                .addComponentFillVertically(new JBScrollPane(queryResultTable), 8)
                .addComponent(statusLabel, 8)
                .addComponent(hintLabel, 4)
                .getPanel();
        panel.setPreferredSize(new Dimension(JBUI.scale(880), JBUI.scale(460)));
        installShortcuts(panel);
        refreshStatus();
        return panel;
    }

    @Override
    protected @Nullable JComponent createSouthPanel() {
        JComponent south = super.createSouthPanel();
        if (south != null) {
            south.setBorder(JBUI.Borders.emptyTop(8));
        }
        return south;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction()};
    }

    @Override
    protected Action @NotNull [] createLeftSideActions() {
        return new Action[]{new DialogWrapperAction(DataPivotBundle.message("data.pivot.query.copy.row")) {
            @Override
            protected void doAction(java.awt.event.ActionEvent e) {
                copySelectedRow();
            }
        }};
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return remoteSearchField == null ? null : remoteSearchField.getTextEditor();
    }

    @Override
    protected @Nullable String getDimensionServiceKey() {
        return "data-pivot.query.dialog";
    }

    private void installShortcuts(@NotNull JComponent panel) {
        // HeadlessToolkit.getMenuShortcutKeyMaskEx() throws HeadlessException (CI ubuntu).
        int menuMask = SystemInfo.isMac ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK;
        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                localSearchField.getTextEditor().requestFocusInWindow();
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_F, menuMask)), panel);

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                remoteSearchField.getTextEditor().requestFocusInWindow();
            }
        }.registerCustomShortcutSet(
                new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_F, menuMask | KeyEvent.SHIFT_DOWN_MASK)),
                panel);

        DumbAwareAction copyAction = new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                copySelectedRow();
            }
        };
        copyAction.registerCustomShortcutSet(CommonShortcuts.getCopy(), queryResultTable);
        copyAction.registerCustomShortcutSet(
                new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)),
                queryResultTable);
    }

    private void scheduleLocalSearch() {
        localSearchAlarm.cancelAllRequests();
        localSearchAlarm.addRequest(this::handleLocalSearchInput, SEARCH_DEBOUNCE_MS);
    }

    private void scheduleRemoteSearch() {
        remoteSearchAlarm.cancelAllRequests();
        remoteSearchAlarm.addRequest(() -> updateTable(remoteSearchField.getText()), SEARCH_DEBOUNCE_MS);
    }

    public void performLocalSearch(@Nullable String query) {
        localSearchAlarm.cancelAllRequests();
        if (query == null) {
            query = "";
        }
        localSearchField.setText(query);
        handleLocalSearchInput();
    }

    public void performRemoteSearch(@Nullable String query) {
        remoteSearchAlarm.cancelAllRequests();
        if (query == null) {
            query = "";
        }
        remoteSearchField.setText(query);
        updateTable(query);
    }

    public boolean copySelectedRow() {
        int selectedViewRow = queryResultTable.getSelectedRow();
        if (selectedViewRow < 0) {
            statusLabel.setText(DataPivotBundle.message("data.pivot.query.status.no.selection"));
            return false;
        }
        int modelRow = queryResultTable.convertRowIndexToModel(selectedViewRow);
        QueryTableRow row = resultTableModel.getQueryTableRow(modelRow);
        lastCopiedText = JSONUtil.toJsonStr(row.getData());
        DataPivotUi.copyText(lastCopiedText);
        statusLabel.setText(DataPivotBundle.message("data.pivot.query.status.copied"));
        return true;
    }

    void handleLocalSearchInput() {
        if (localSearchField == null || queryResultTable == null || resultTableModel == null) {
            return;
        }
        String query = localSearchField.getText();
        clearHighlights();
        if (StrUtil.isEmpty(query)) {
            queryResultTable.repaint();
            return;
        }
        for (int row = 0; row < resultTableModel.getRowCount(); row++) {
            for (int col = 0; col < resultTableModel.getColumnCount(); col++) {
                Object value = resultTableModel.getValueAt(row, col);
                if (String.valueOf(value).contains(query)) {
                    highlightedCells.add(new Pair<>(row, col));
                }
            }
        }
        queryResultTable.repaint();
    }

    void updateTable(@Nullable String query) {
        if (statusLabel != null) {
            statusLabel.setText(DataPivotBundle.message("data.pivot.query.status.searching"));
        }
        if (queryResultTable != null) {
            queryResultTable.getEmptyText().setText(DataPivotBundle.message("data.pivot.query.status.searching"));
        }
        if (StrUtil.isEmpty(query)) {
            databaseQueryConfig.setLikeValue(null);
        } else {
            databaseQueryConfig.setLikeValue(query);
        }
        int generation = queryGeneration.incrementAndGet();
        BackgroundQuerySupport.execute(
                project,
                DataPivotBundle.message("data.pivot.query.title"),
                () -> queryRunner.query(databaseQueryConfig),
                result -> applyQueryResult(generation, result)
        );
    }

    private void applyQueryResult(int generation, @NotNull BackgroundQuerySupport.Result result) {
        if (generation != queryGeneration.get()) {
            return;
        }
        if (result.isCancelled()) {
            if (statusLabel != null) {
                statusLabel.setText(DataPivotBundle.message("data.pivot.query.status.cancelled"));
            }
            return;
        }
        if (result.isError()) {
            applyRows(List.of());
            if (statusLabel != null) {
                statusLabel.setText(DataPivotBundle.message("data.pivot.query.status.error"));
            }
            MessageUtil.Notice.error(result.getError());
            return;
        }
        applyRows(result.getRows());
        handleLocalSearchInput();
        refreshStatus();
    }

    private void applyRows(@NotNull List<Map<String, Object>> updatedResults) {
        String[] columnNames = resolveColumnNames(updatedResults);
        resultTableModel.updateData(toRows(updatedResults, columnNames), columnNames);
        if (queryResultTable != null) {
            DataPivotUi.applyFixedColumnWidths(queryResultTable);
        }
    }

    private void refreshStatus() {
        int rows = resultTableModel.getRowCount();
        if (rows == 0) {
            statusLabel.setText(DataPivotBundle.message("data.pivot.query.status.empty"));
        } else {
            statusLabel.setText(DataPivotBundle.message("data.pivot.query.status.rows", rows));
        }
        queryResultTable.getEmptyText().setText(DataPivotBundle.message("data.pivot.query.empty"));
    }

    private String statusText() {
        int rows = resultTableModel.getRowCount();
        return rows == 0
                ? DataPivotBundle.message("data.pivot.query.status.empty")
                : DataPivotBundle.message("data.pivot.query.status.rows", rows);
    }

    private void clearHighlights() {
        highlightedCells.clear();
    }

    private String[] resolveColumnNames(@NotNull List<Map<String, Object>> queryResults) {
        if (!queryResults.isEmpty()) {
            Set<String> names = new LinkedHashSet<>(queryResults.get(0).keySet());
            return names.toArray(new String[0]);
        }
        List<String> columns = databaseQueryConfig.getColumns();
        if (columns == null || columns.isEmpty() || (columns.size() == 1 && "*".equals(columns.get(0)))) {
            return new String[0];
        }
        return columns.toArray(new String[0]);
    }

    private static List<QueryTableRow> toRows(@NotNull List<Map<String, Object>> queryResults, String[] columnNames) {
        Set<String> columnSet = Arrays.stream(columnNames).collect(Collectors.toCollection(LinkedHashSet::new));
        return queryResults.stream()
                .map(result -> new QueryTableRow(result, columnSet))
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
    }

    private static @NotNull String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    public JBTable getQueryResultTable() {
        return queryResultTable;
    }

    public SearchTextField getLocalSearchField() {
        return localSearchField;
    }

    public SearchTextField getRemoteSearchField() {
        return remoteSearchField;
    }

    public JBLabel getStatusLabel() {
        return statusLabel;
    }

    public JBLabel getHintLabel() {
        return hintLabel;
    }

    public String getLastCopiedText() {
        return lastCopiedText;
    }

    public int getHighlightedCellCount() {
        return highlightedCells.size();
    }

    public ResultTableModel getResultTableModel() {
        return resultTableModel;
    }

    static final class ResultTableModel extends AbstractTableModel {
        private List<QueryTableRow> data;
        private String[] columnNames;

        ResultTableModel(List<QueryTableRow> data, String[] columnNames) {
            this.data = data;
            this.columnNames = columnNames;
        }

        void updateData(List<QueryTableRow> newData, String[] columnNames) {
            boolean structureChanged = !Arrays.equals(this.columnNames, columnNames);
            this.data = newData;
            this.columnNames = columnNames;
            if (structureChanged) {
                fireTableStructureChanged();
            } else {
                fireTableDataChanged();
            }
        }

        QueryTableRow getQueryTableRow(int rowIndex) {
            return data.get(rowIndex);
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            QueryTableRow rowData = data.get(rowIndex);
            return rowData.getData().get(columnNames[columnIndex]);
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
    }

    static final class QueryTableRow {
        private final Map<String, Object> data;
        private final Set<String> columns;

        QueryTableRow(Map<String, Object> data, Set<String> columns) {
            this.data = data;
            this.columns = columns;
        }

        Map<String, Object> getData() {
            return data;
        }
    }

    private class HighlightRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            int modelRow = table.convertRowIndexToModel(row);
            int modelColumn = table.convertColumnIndexToModel(column);
            if (isSelected) {
                cell.setBackground(table.getSelectionBackground());
                cell.setForeground(table.getSelectionForeground());
            } else if (isHighlighted(modelRow, modelColumn)) {
                cell.setBackground(DataPivotUi.HIGHLIGHT_COLOR);
                cell.setForeground(table.getForeground());
            } else {
                cell.setBackground(table.getBackground());
                cell.setForeground(table.getForeground());
            }
            return cell;
        }

        private boolean isHighlighted(int row, int column) {
            for (Pair<Integer, Integer> cell : highlightedCells) {
                if (cell.getFirst() == row && cell.getSecond() == column) {
                    return true;
                }
            }
            return false;
        }
    }
}
