package com.data.pivot.plugin.view.report;

import com.data.pivot.plugin.i18n.DataPivotBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class AnalysisResultModel {
    public static final String COUNT_COLUMN = "rs_count";
    public static final String PERCENTAGE_COLUMN = "percentage";

    private AnalysisResultModel() {
    }

    public static @NotNull List<AnalysisRow> fromMaps(@Nullable List<Map<String, Object>> maps,
                                                      @Nullable String conditionField) {
        if (maps == null || maps.isEmpty() || conditionField == null || conditionField.isBlank()) {
            return List.of();
        }
        List<AnalysisRow> rows = new ArrayList<>(maps.size());
        for (Map<String, Object> map : maps) {
            if (map == null) {
                continue;
            }
            rows.add(new AnalysisRow(
                    stringify(map.get(conditionField)),
                    stringify(map.get(COUNT_COLUMN)),
                    stringify(map.get(PERCENTAGE_COLUMN))
            ));
        }
        return Collections.unmodifiableList(rows);
    }

    public static @NotNull String formatInfo(@NotNull AnalysisRow row) {
        return DataPivotBundle.message("data.pivot.analysis.info", row.getCount(), row.getPercentage());
    }

    public static @NotNull DataPivotLookupElement toLookupElement(@NotNull AnalysisRow row,
                                                                  @NotNull String selectedText,
                                                                  @NotNull String sql) {
        return new DataPivotLookupElement(selectedText, row.getValue(), sql, formatInfo(row));
    }

    public static @NotNull List<DataPivotLookupElement> toLookupElements(@NotNull List<AnalysisRow> rows,
                                                                         @NotNull String selectedText,
                                                                         @NotNull String sql) {
        List<DataPivotLookupElement> elements = new ArrayList<>(rows.size());
        for (AnalysisRow row : rows) {
            elements.add(toLookupElement(row, selectedText, sql));
        }
        return elements;
    }

    private static @NotNull String stringify(@Nullable Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
