package com.data.pivot.plugin.mapping;

import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import com.intellij.database.psi.DbColumn;
import com.intellij.database.psi.DbTable;
import org.jetbrains.annotations.Nullable;

public final class MappingHit {
    private final MappingConfidence confidence;
    private final DbTable table;
    private final DbColumn column;
    private final DataPivotMappingSettingInfo setting;

    public MappingHit(
            MappingConfidence confidence,
            @Nullable DbTable table,
            @Nullable DbColumn column,
            @Nullable DataPivotMappingSettingInfo setting
    ) {
        this.confidence = confidence;
        this.table = table;
        this.column = column;
        this.setting = setting;
    }

    public static MappingHit unresolved() {
        return new MappingHit(MappingConfidence.UNRESOLVED, null, null, null);
    }

    public static MappingHit ambiguous(@Nullable DataPivotMappingSettingInfo setting) {
        return new MappingHit(MappingConfidence.AMBIGUOUS, null, null, setting);
    }

    public MappingConfidence getConfidence() {
        return confidence;
    }

    public @Nullable DbTable getTable() {
        return table;
    }

    public @Nullable DbColumn getColumn() {
        return column;
    }

    public @Nullable DataPivotMappingSettingInfo getSetting() {
        return setting;
    }

    public boolean isAmbiguous() {
        return confidence == MappingConfidence.AMBIGUOUS;
    }

    public boolean isHighConfidence() {
        return confidence == MappingConfidence.EXACT || confidence == MappingConfidence.NAMING;
    }

    public boolean isUsableForQuery() {
        return table != null && (isHighConfidence() || confidence == MappingConfidence.FUZZY);
    }

    public MappingHit withColumn(@Nullable DbColumn resolvedColumn, MappingConfidence columnConfidence) {
        MappingConfidence combined = combine(confidence, columnConfidence);
        return new MappingHit(combined, table, resolvedColumn, setting);
    }

    private static MappingConfidence combine(MappingConfidence table, MappingConfidence column) {
        if (table == MappingConfidence.AMBIGUOUS || column == MappingConfidence.AMBIGUOUS) {
            return MappingConfidence.AMBIGUOUS;
        }
        if (table == MappingConfidence.UNRESOLVED || column == MappingConfidence.UNRESOLVED) {
            return MappingConfidence.UNRESOLVED;
        }
        if (table == MappingConfidence.FUZZY || column == MappingConfidence.FUZZY) {
            return MappingConfidence.FUZZY;
        }
        if (table == MappingConfidence.NAMING || column == MappingConfidence.NAMING) {
            return MappingConfidence.NAMING;
        }
        return MappingConfidence.EXACT;
    }
}
