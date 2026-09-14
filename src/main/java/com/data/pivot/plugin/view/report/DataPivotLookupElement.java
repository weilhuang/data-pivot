package com.data.pivot.plugin.view.report;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import org.jetbrains.annotations.NotNull;

public class DataPivotLookupElement extends LookupElement {
    private final String myData;
    private final String text;
    private final String sql;
    private final String info;

    public DataPivotLookupElement(String text, String data, String sql, String info) {
        this.myData = data;
        this.text = text;
        this.sql = sql;
        this.info = info;
    }

    @Override
    public @NotNull String getLookupString() {
        return text;
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(myData);
        presentation.setTypeText(info);
    }

    public String getData() {
        return myData;
    }

    public String getSql() {
        return sql;
    }

    public String getInfo() {
        return info;
    }
}
