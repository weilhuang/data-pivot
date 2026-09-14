package com.data.pivot.plugin.view.setting;

import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class MappingSettingSupportTest {
    @Test
    public void copyAllCreatesIndependentSnapshot() {
        DataPivotMappingSettingInfo original = item("app", "com.example", "ds/demo", "JPA");
        List<DataPivotMappingSettingInfo> copy = MappingSettingSupport.copyAll(List.of(original));

        assertEquals(1, copy.size());
        assertNotSame(original, copy.get(0));
        assertTrue(MappingSettingSupport.sameItem(original, copy.get(0)));

        copy.get(0).setStrategyCode("MybatisPlus");
        assertFalse(MappingSettingSupport.sameItem(original, copy.get(0)));
        assertFalse(MappingSettingSupport.same(List.of(original), copy));
    }

    @Test
    public void sameDetectsSizeAndFieldDifferences() {
        DataPivotMappingSettingInfo first = item("app", "com.example", "ds/demo", "JPA");
        DataPivotMappingSettingInfo second = item("app", "com.example", "ds/demo", "JPA");

        assertTrue(MappingSettingSupport.same(List.of(first), List.of(second)));
        assertFalse(MappingSettingSupport.same(List.of(first), List.of()));
        second.setPackageName("com.other");
        assertFalse(MappingSettingSupport.same(List.of(first), List.of(second)));
    }

    @Test
    public void copyAllTreatsNullAsEmpty() {
        assertTrue(MappingSettingSupport.copyAll(null).isEmpty());
        assertTrue(MappingSettingSupport.same(null, List.of()));
    }

    private static DataPivotMappingSettingInfo item(String module, String pkg, String database, String strategy) {
        DataPivotMappingSettingInfo info = new DataPivotMappingSettingInfo();
        info.setModelName(module);
        info.setPackageName(pkg);
        info.setDatabasePath(database);
        info.setStrategyCode(strategy);
        info.setPackageReference(module + "/" + pkg);
        info.setDatabaseReference("uid." + database);
        return info;
    }
}
