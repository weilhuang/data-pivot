package com.data.pivot.plugin.mapping;

import com.data.pivot.plugin.DataPivotPlatformTestCase;
import com.data.pivot.plugin.context.DataPivotApplication;
import com.data.pivot.plugin.entity.DataPivotDatabaseInfo;
import com.data.pivot.plugin.entity.DataPivotMappingSettingInfo;
import com.data.pivot.plugin.entity.custom.DataPivotStrategyInfo;
import com.data.pivot.plugin.tool.DataPivotUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.testFramework.JUnit38AssumeSupportRunner;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Mapping settings/strategy/database-info maps are project services. The default project
 * stands in for a second open window: lookups must follow the passed {@link Project},
 * not {@code ProjectUtils.getCurrProject()}.
 */
@RunWith(JUnit38AssumeSupportRunner.class)
public class MappingResolverProjectIntegrationTest extends DataPivotPlatformTestCase {
    public void testSettingsStrategyAndDatabaseMapsUsePassedProject() {
        Project psiProject = getProject();
        Project otherProject = ProjectManager.getInstance().getDefaultProject();
        assertNotSame("need two project-scoped DataPivotApplication instances", psiProject, otherProject);

        DataPivotApplication psiApp = DataPivotApplication.getInstance(psiProject);
        DataPivotApplication otherApp = DataPivotApplication.getInstance(otherProject);
        assertNotSame(psiApp, otherApp);

        psiApp.CACHE.DP_MAPPING_SETTING_INFO_LIST_CACHE.update(List.of(
                setting("app", "com.example", "psi-db", "psi-ds.psi-db")));
        otherApp.CACHE.DP_MAPPING_SETTING_INFO_LIST_CACHE.update(List.of(
                setting("app", "com.example", "other-db", "other-ds.other-db")));

        assertEquals("psi-db", DataPivotUtil.getObjectDataPivotSettingInfo(
                psiProject, "app/com.example.user").getDatabaseName());
        assertEquals("other-db", DataPivotUtil.getObjectDataPivotSettingInfo(
                otherProject, "app/com.example.user").getDatabaseName());
        assertEquals("psi-db", DataPivotUtil.getRelationDataPivotSettingInfo(
                psiProject, "psi-ds.psi-db").getDatabaseName());
        assertEquals("other-db", DataPivotUtil.getRelationDataPivotSettingInfo(
                otherProject, "other-ds.other-db").getDatabaseName());

        psiApp.MAPPER.DEFAULT_STRATEGY_MAPPER.put("JPA", strategy("JPA", "psi-jpa"));
        otherApp.MAPPER.DEFAULT_STRATEGY_MAPPER.put("JPA", strategy("JPA", "other-jpa"));
        assertEquals("psi-jpa", DataPivotApplication.getDataPivotStrategyInfo(psiProject, "JPA").getName());
        assertEquals("other-jpa", DataPivotApplication.getDataPivotStrategyInfo(otherProject, "JPA").getName());

        psiApp.MAPPER.DP_DR_DATABASE_MAPPER.put("shared-ref.demo", databaseInfo("shared-ref.demo", "psi-ds"));
        otherApp.MAPPER.DP_DR_DATABASE_MAPPER.put("shared-ref.demo", databaseInfo("shared-ref.demo", "other-ds"));
        DataPivotMappingSettingInfo uniqueSetting = new DataPivotMappingSettingInfo();
        uniqueSetting.setDatabaseReference("shared-ref.demo");
        uniqueSetting.setDatabaseName("demo");
        assertEquals("psi-ds", MappingResolver.settingUniqueId(psiProject, uniqueSetting));
        assertEquals("other-ds", MappingResolver.settingUniqueId(otherProject, uniqueSetting));
    }

    public void testNoArgSettingsLookupStillReadsCurrentProjectApplication() {
        DataPivotApplication.getInstance().CACHE.DP_MAPPING_SETTING_INFO_LIST_CACHE.update(List.of(
                setting("app", "com.example", "curr-db", "curr.curr-db")));

        assertEquals("curr-db", DataPivotUtil.getObjectDataPivotSettingInfo("app/com.example.user").getDatabaseName());
        assertEquals("curr-db", DataPivotUtil.getObjectDataPivotSettingInfo(null, "app/com.example.user").getDatabaseName());
    }

    private static DataPivotMappingSettingInfo setting(
            String module, String pkg, String databaseName, String databaseReference) {
        DataPivotMappingSettingInfo info = new DataPivotMappingSettingInfo();
        info.setModelName(module);
        info.setPackageName(pkg);
        info.setPackageReference(module + "/" + pkg);
        info.setDataSourceName("ds");
        info.setDatabaseName(databaseName);
        info.setDatabaseReference(databaseReference);
        info.setStrategyCode("JPA");
        return info;
    }

    private static DataPivotStrategyInfo strategy(String code, String name) {
        DataPivotStrategyInfo info = new DataPivotStrategyInfo();
        info.setCode(code);
        info.setName(name);
        return info;
    }

    private static DataPivotDatabaseInfo databaseInfo(String reference, String uniqueId) {
        DataPivotDatabaseInfo info = new DataPivotDatabaseInfo();
        info.setDatabaseReference(reference);
        info.setUniqueId(uniqueId);
        return info;
    }
}
