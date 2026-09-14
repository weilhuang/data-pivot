package com.data.pivot.plugin.view.ui;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.Color;
import java.awt.datatransfer.StringSelection;

/**
 * Shared IntelliJ Platform UI helpers for data-pivot dialogs and settings.
 */
public final class DataPivotUi {
    public static final Color HIGHLIGHT_COLOR = new JBColor(new Color(0xFFF9C4), new Color(0x4C502B));

    private DataPivotUi() {
    }

    public static void configureTable(@NotNull JBTable table, @NotNull String emptyText) {
        table.setStriped(true);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setRowHeight(JBUI.scale(22));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getEmptyText().setText(emptyText);
        table.setFillsViewportHeight(true);
    }

    public static @NotNull JBLabel comment(@NotNull String text) {
        JBLabel label = new JBLabel(text);
        label.setAllowAutoWrapping(true);
        label.setForeground(JBColor.namedColor("Label.infoForeground", UIUtil.getContextHelpForeground()));
        return label;
    }

    public static @NotNull JBLabel status(@NotNull String text) {
        JBLabel label = new JBLabel(text);
        label.setForeground(UIUtil.getContextHelpForeground());
        return label;
    }

    public static @NotNull SearchTextField searchField(@NotNull String accessibleName,
                                                       @NotNull String placeholder,
                                                       @NotNull String tooltip) {
        SearchTextField field = new SearchTextField();
        field.getTextEditor().getEmptyText().setText(placeholder);
        field.getTextEditor().setToolTipText(tooltip);
        field.getTextEditor().getAccessibleContext().setAccessibleName(accessibleName);
        return field;
    }

    public static void copyText(@NotNull String text) {
        CopyPasteManager.getInstance().setContents(new StringSelection(text));
    }

    public static int preferredColumnWidth() {
        return JBUI.scale(120);
    }

    public static void applyFixedColumnWidths(@NotNull JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(preferredColumnWidth());
        }
    }
}
