package com.data.pivot.plugin.view;

import com.data.pivot.plugin.i18n.DataPivotBundle;
import com.data.pivot.plugin.view.ui.DataPivotUi;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.EditableModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class DataPivotTableView<T> extends DefaultTableModel implements EditableModel {

    private List<DataPivotTableColumn<T>> tableColumns;
    private List<T> dataList;
    private BiFunction<List<T>, T, Boolean> checkAddRow;
    private Supplier<DataPivotTableRowView<T>> dataPivotTableRowViewSupplier;
    private JBTable table;

    public DataPivotTableView(List<DataPivotTableColumn<T>> tableColumns, List<T> dataList,
                              Supplier<DataPivotTableRowView<T>> dataPivotTableRowViewSupplier, Class<T> tClass) {
        this.dataPivotTableRowViewSupplier = dataPivotTableRowViewSupplier;
        this.initTableColumns(tableColumns);
        this.initTable();
        this.initDataList(dataList);
    }

    public void removeAllRow() {
        int rowCount = getRowCount();
        for (int i = 0; i < rowCount; i++) {
            super.removeRow(0);
        }
    }

    private Vector<String> toRow(T data) {
        Vector<String> vector = new Vector<>();
        this.tableColumns.stream()
                .map(tableColumn -> String.valueOf(tableColumn.getFieldFun().callWithRuntimeException(data)))
                .forEach(vector::add);
        return vector;
    }

    private void initTableColumns(List<DataPivotTableColumn<T>> tableColumns) {
        this.tableColumns = tableColumns;
        for (DataPivotTableColumn<T> tableColumn : tableColumns) {
            addColumn(tableColumn.getName());
        }
    }

    private void initTable() {
        this.table = new JBTable(this);
        DataPivotUi.configureTable(this.table, DataPivotBundle.message("data.pivot.table.empty"));
        this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private void initDataList(List<T> dataList) {
        this.dataList = dataList == null ? new ArrayList<>() : dataList;
        removeAllRow();
        for (T data : this.dataList) {
            addRowData(data);
        }
    }

    public void reload(List<T> dataList) {
        initDataList(dataList);
    }

    private void addRowData(T dataData) {
        addRow(toRow(dataData));
    }

    public JComponent createPanel() {
        return createPanel(null);
    }

    public JComponent createPanel(@Nullable AnAction extraAction) {
        final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(this.table);
        if (extraAction != null) {
            decorator.addExtraAction(extraAction);
        }
        return decorator.createPanel();
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        if (row < this.dataList.size()) {
            super.setValueAt(value, row, column);
            T obj = this.dataList.get(row);
            if (tableColumns.get(column).getFieldSetter() != null) {
                tableColumns.get(column).getFieldSetter().accept(obj, value);
            }
        }
    }

    @Override
    public void removeRow(int row) {
        super.removeRow(row);
        this.dataList.remove(row);
    }

    @Override
    public void addRow() {
        DataPivotTableRowView<T> dataPivotTableRowView = dataPivotTableRowViewSupplier.get();
        if (!dataPivotTableRowView.showAndGet()) {
            return;
        }
        T entity = dataPivotTableRowView.getValue();
        if (checkAddRow == null || checkAddRow.apply(dataList, entity)) {
            this.dataList.add(entity);
            addRowData(entity);
        }
    }

    @Override
    public void exchangeRows(int oldIndex, int newIndex) {
        super.moveRow(oldIndex, oldIndex, newIndex);
        T remove = this.dataList.remove(oldIndex);
        this.dataList.add(newIndex, remove);
    }

    @Override
    public boolean canExchangeRows(int oldIndex, int newIndex) {
        return true;
    }

    public List<T> getDataList() {
        return dataList;
    }

    public void setDataList(List<T> dataList) {
        this.dataList = dataList;
    }

    public JBTable getTable() {
        return table;
    }

    public void setTable(JBTable table) {
        this.table = table;
    }

    public BiFunction<List<T>, T, Boolean> getCheckAddRow() {
        return checkAddRow;
    }

    public void setCheckAddRow(BiFunction<List<T>, T, Boolean> checkAddRow) {
        this.checkAddRow = checkAddRow;
    }
}
