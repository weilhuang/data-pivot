package com.data.pivot.plugin.mapping;

import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MappingResolverTest {
    @Test
    public void settingUniqueIdParsesDatabaseReference() {
        DataPivotMappingSettingInfo setting = new DataPivotMappingSettingInfo();
        setting.setDatabaseReference("datasource-uuid.demo");
        setting.setDatabaseName("demo");

        assertEquals("datasource-uuid", MappingResolver.settingUniqueId(setting));
    }

    @Test
    public void settingUniqueIdReturnsNullWhenReferenceIsMissing() {
        assertNull(MappingResolver.settingUniqueId(null));
        assertNull(MappingResolver.settingUniqueId(new DataPivotMappingSettingInfo()));
    }
}
