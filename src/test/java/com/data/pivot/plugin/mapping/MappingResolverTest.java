package com.data.pivot.plugin.mapping;

import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

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

    @Test
    public void stampCacheMissesWhenModificationStampChanges() {
        MappingResolver.StampCache cache = new MappingResolver.StampCache();
        MappingHit first = MappingHit.unresolved();
        cache.put("com.example.SysUser", 1L, first);

        assertSame(first, cache.getIfFresh("com.example.SysUser", 1L));
        assertNull("annotation/source edits change the file stamp and must not reuse the old table",
                cache.getIfFresh("com.example.SysUser", 2L));
    }
}
