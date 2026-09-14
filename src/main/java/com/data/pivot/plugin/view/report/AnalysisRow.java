package com.data.pivot.plugin.view.report;

import org.jetbrains.annotations.NotNull;

public final class AnalysisRow {
    private final String value;
    private final String count;
    private final String percentage;

    public AnalysisRow(@NotNull String value, @NotNull String count, @NotNull String percentage) {
        this.value = value;
        this.count = count;
        this.percentage = percentage;
    }

    public @NotNull String getValue() {
        return value;
    }

    public @NotNull String getCount() {
        return count;
    }

    public @NotNull String getPercentage() {
        return percentage;
    }
}
