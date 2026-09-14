package com.data.pivot.plugin.view.report;

import com.data.pivot.plugin.entity.DatabaseQueryConfig;
import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.data.pivot.plugin.tool.BackgroundQuerySupport;
import com.data.pivot.plugin.tool.MessageUtil;
import com.data.pivot.plugin.view.query.QueryRunner;
import com.data.pivot.plugin.view.ui.DataPivotUi;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.AbstractTableModel;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class AnalysisResultComponent extends DialogWrapper {
    private final DatabaseQueryConfig databaseQueryConfig;
    private final List<AnalysisRow> rows;
    private final String sql;
    private final Project project;

    private JBTable resultTable;
    private AnalysisTableModel tableModel;
    private JBLabel statusLabel;
    private JBLabel hintLabel;
    private JBTextArea sqlArea;
    private String lastCopiedText = "";

    public AnalysisResultComponent(@Nullable Project project,
                                   @NotNull DatabaseQueryConfig databaseQueryConfig,
                                   @NotNull List<AnalysisRow> rows,
                                   @NotNull String sql) {
        super(project, false);
        this.project = project;
        this.databaseQueryConfig = databaseQueryConfig;
        this.rows = new ArrayList<>(rows);
        this.sql = sql;
        setTitle(DataPivotBundle.message(
                "data.pivot.analysis.title.detail",
                nullToEmpty(databaseQueryConfig.getTableName()),
                nullToEmpty(databaseQueryConfig.getConditionField())));
        setModal(false);
        setOKButtonText(DataPivotBundle.message("data.pivot.dialog.close"));
        init();
        setSize(JBUI.scale(720), JBUI.scale(520));
    }

    @Override
    protected @NotNull JComponent createCenterPanel() {
        tableModel = new AnalysisTableModel(this.rows);
        resultTable = new JBTable(tableModel);
        DataPivotUi.configureTable(resultTable, DataPivotBundle.message("data.pivot.analysis.empty"));
        resultTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        resultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    copySelectedValue();
                }
            }
        });
        DumbAwareAction copyValueAction = new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                copySelectedValue();
            }
        };
        copyValueAction.registerCustomShortcutSet(CommonShortcuts.getCopy(), resultTable);

        sqlArea = new JBTextArea(sql, 4, 40);
        sqlArea.setEditable(false);
        sqlArea.setLineWrap(true);
        sqlArea.setWrapStyleWord(true);
        sqlArea.getAccessibleContext().setAccessibleName(DataPivotBundle.message("data.pivot.analysis.sql.label"));

        statusLabel = DataPivotUi.status(statusText());
        hintLabel = DataPivotUi.comment(DataPivotBundle.message("data.pivot.analysis.hint"));

        JPanel panel = FormBuilder.createFormBuilder()
                .addComponentFillVertically(new JBScrollPane(resultTable), 0)
                .addLabeledComponent(DataPivotBundle.message("data.pivot.analysis.sql.label"), new JBScrollPane(sqlArea), 8)
                .addComponent(statusLabel, 8)
                .addComponent(hintLabel, 4)
                .getPanel();
        panel.setPreferredSize(new Dimension(JBUI.scale(700), JBUI.scale(460)));
        return panel;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction()};
    }

    @Override
    protected Action @NotNull [] createLeftSideActions() {
        return new Action[]{
                new DialogWrapperAction(DataPivotBundle.message("data.pivot.analysis.copy.sql")) {
                    @Override
                    protected void doAction(java.awt.event.ActionEvent e) {
                        copySql();
                    }
                },
                new DialogWrapperAction(DataPivotBundle.message("data.pivot.analysis.copy.value")) {
                    @Override
                    protected void doAction(java.awt.event.ActionEvent e) {
                        copySelectedValue();
                    }
                }
        };
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return resultTable;
    }

    @Override
    protected @Nullable String getDimensionServiceKey() {
        return "data-pivot.analysis.dialog";
    }

    public void loadResults(@NotNull QueryRunner queryRunner) {
        if (statusLabel != null) {
            statusLabel.setText(DataPivotBundle.message("data.pivot.analysis.status.searching"));
        }
        if (resultTable != null) {
            resultTable.getEmptyText().setText(DataPivotBundle.message("data.pivot.analysis.status.searching"));
        }
        BackgroundQuerySupport.execute(
                project,
                DataPivotBundle.message("data.pivot.analysis.title"),
                () -> queryRunner.query(databaseQueryConfig),
                this::applyQueryResult
        );
    }

    private void applyQueryResult(@NotNull BackgroundQuerySupport.Result result) {
        if (result.isCancelled()) {
            if (statusLabel != null) {
                statusLabel.setText(DataPivotBundle.message("data.pivot.analysis.status.cancelled"));
            }
            return;
        }
        if (result.isError()) {
            rows.clear();
            if (tableModel != null) {
                tableModel.fireTableDataChanged();
            }
            if (statusLabel != null) {
                statusLabel.setText(DataPivotBundle.message("data.pivot.analysis.status.error"));
            }
            MessageUtil.Notice.error(result.getError());
            return;
        }
        List<AnalysisRow> next = AnalysisResultModel.fromMaps(result.getRows(), databaseQueryConfig.getConditionField());
        rows.clear();
        rows.addAll(next);
        if (tableModel != null) {
            tableModel.fireTableDataChanged();
        }
        if (statusLabel != null) {
            statusLabel.setText(statusText());
        }
        if (resultTable != null) {
            resultTable.getEmptyText().setText(DataPivotBundle.message("data.pivot.analysis.empty"));
        }
    }

    public boolean copySql() {
        lastCopiedText = sql;
        DataPivotUi.copyText(sql);
        statusLabel.setText(DataPivotBundle.message("data.pivot.analysis.copied.sql"));
        return true;
    }

    public boolean copySelectedValue() {
        int selectedViewRow = resultTable.getSelectedRow();
        if (selectedViewRow < 0) {
            return false;
        }
        int modelRow = resultTable.convertRowIndexToModel(selectedViewRow);
        lastCopiedText = rows.get(modelRow).getValue();
        DataPivotUi.copyText(lastCopiedText);
        statusLabel.setText(DataPivotBundle.message("data.pivot.analysis.copied.value"));
        return true;
    }

    private String statusText() {
        if (rows.isEmpty()) {
            return DataPivotBundle.message("data.pivot.analysis.empty");
        }
        return DataPivotBundle.message("data.pivot.analysis.status.rows", rows.size());
    }

    private static @NotNull String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    public JBTable getResultTable() {
        return resultTable;
    }

    public JBLabel getStatusLabel() {
        return statusLabel;
    }

    public JBLabel getHintLabel() {
        return hintLabel;
    }

    public JBTextArea getSqlArea() {
        return sqlArea;
    }

    public String getLastCopiedText() {
        return lastCopiedText;
    }

    public List<AnalysisRow> getRows() {
        return rows;
    }

    public DatabaseQueryConfig getDatabaseQueryConfig() {
        return databaseQueryConfig;
    }

    private static final class AnalysisTableModel extends AbstractTableModel {
        private final List<AnalysisRow> rows;
        private final String[] columns = {
                DataPivotBundle.message("data.pivot.analysis.column.value"),
                DataPivotBundle.message("data.pivot.analysis.column.count"),
                DataPivotBundle.message("data.pivot.analysis.column.percentage")
        };

        private AnalysisTableModel(List<AnalysisRow> rows) {
            this.rows = rows;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            AnalysisRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.getValue();
                case 1 -> row.getCount();
                case 2 -> row.getPercentage();
                default -> "";
            };
        }
    }
}
