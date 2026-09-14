package com.data.pivot.plugin.view.setting;

import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MappingSettingSupport {
    private MappingSettingSupport() {
    }

    public static @NotNull List<DataPivotMappingSettingInfo> copyAll(
            @Nullable List<DataPivotMappingSettingInfo> source) {
        List<DataPivotMappingSettingInfo> copy = new ArrayList<>();
        if (source == null) {
            return copy;
        }
        for (DataPivotMappingSettingInfo item : source) {
            if (item != null) {
                copy.add(copyOf(item));
            }
        }
        return copy;
    }

    public static @NotNull DataPivotMappingSettingInfo copyOf(@NotNull DataPivotMappingSettingInfo source) {
        DataPivotMappingSettingInfo copy = new DataPivotMappingSettingInfo();
        copy.setModelName(source.getModelName());
        copy.setPackageName(source.getPackageName());
        copy.setDataSourceName(source.getDataSourceName());
        copy.setDatabaseName(source.getDatabaseName());
        copy.setDatabasePath(source.getDatabasePath());
        copy.setStrategyCode(source.getStrategyCode());
        copy.setDatabaseReference(source.getDatabaseReference());
        copy.setPackageReference(source.getPackageReference());
        copy.setSqlCode(source.getSqlCode());
        copy.setDataPivotCustomSqlInfo(source.getDataPivotCustomSqlInfo());
        return copy;
    }

    public static boolean same(@Nullable List<DataPivotMappingSettingInfo> left,
                               @Nullable List<DataPivotMappingSettingInfo> right) {
        List<DataPivotMappingSettingInfo> leftList = left == null ? List.of() : left;
        List<DataPivotMappingSettingInfo> rightList = right == null ? List.of() : right;
        if (leftList.size() != rightList.size()) {
            return false;
        }
        for (int i = 0; i < leftList.size(); i++) {
            if (!sameItem(leftList.get(i), rightList.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean sameItem(@Nullable DataPivotMappingSettingInfo left,
                                   @Nullable DataPivotMappingSettingInfo right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(left.getModelName(), right.getModelName())
                && Objects.equals(left.getPackageName(), right.getPackageName())
                && Objects.equals(left.getDatabasePath(), right.getDatabasePath())
                && Objects.equals(left.getStrategyCode(), right.getStrategyCode())
                && Objects.equals(left.getPackageReference(), right.getPackageReference())
                && Objects.equals(left.getDatabaseReference(), right.getDatabaseReference());
    }
}
