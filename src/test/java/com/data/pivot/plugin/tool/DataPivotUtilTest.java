package com.data.pivot.plugin.tool;

import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DataPivotUtilTest {
    @Test
    public void createReferencesJoinModulePackageAndDatabaseSegments() {
        assertEquals("app/com.example", DataPivotUtil.createPackageReference("app", "com.example"));
        assertEquals("app/com.example.User", DataPivotUtil.createClassReference("app", "com.example", "User"));
        assertEquals("app/com.example.User#id", DataPivotUtil.createFieldReference("app", "com.example", "User", "id"));
        assertEquals("ds-1.demo", DataPivotUtil.createDatabaseReference("ds-1", "demo"));
        assertEquals("ds-1.demo.user_account", DataPivotUtil.createTableReference("ds-1", "demo", "user_account"));
        assertEquals("ds-1.demo.user_account.id",
                DataPivotUtil.createColumnReference("ds-1", "demo", "user_account", "id"));
        assertEquals("demo.user_account.id", DataPivotUtil.createColumnPath("demo", "user_account", "id"));
    }

    @Test
    public void compareReferenceMatchesPrefix() {
        assertTrue(DataPivotUtil.compareReference("app/com.example", "app/com.example.user"));
        assertFalse(DataPivotUtil.compareReference("app/com.other", "app/com.example.user"));
        assertTrue(DataPivotUtil.comparePackageName("com.example", "com.example.user"));
    }

    @Test
    public void mappingSettingInfoRoundTripFields() {
        DataPivotMappingSettingInfo info = new DataPivotMappingSettingInfo();
        info.setModelName("app");
        info.setPackageName("com.example");
        info.setPackageReference(DataPivotUtil.createPackageReference("app", "com.example"));
        assertEquals("app/com.example", info.getPackageReference());
    }
}
