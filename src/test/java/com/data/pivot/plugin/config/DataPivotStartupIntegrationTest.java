package com.data.pivot.plugin.config;

import com.data.pivot.plugin.DataPivotPlatformTestCase;
import com.data.pivot.plugin.context.DataPivotApplication;
import com.data.pivot.plugin.enums.DefaultStrategyType;
import com.intellij.testFramework.JUnit38AssumeSupportRunner;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

@RunWith(JUnit38AssumeSupportRunner.class)
public class DataPivotStartupIntegrationTest extends DataPivotPlatformTestCase {
    public void testProjectServiceCanBeResolvedInPlatformProject() {
        DataPivotApplication application = DataPivotApplication.getInstance(getProject());

        assertNotNull(application);
        assertSame(getProject(), application.getProject());
    }

    public void testDefaultStrategiesAreInitializedForProjectApplication() {
        DataPivotApplication application = DataPivotApplication.getInstance(getProject());
        application.MAPPER.DEFAULT_STRATEGY_MAPPER.clear();

        DataPivotInitializer.initDefaultStrategy(application);

        assertTrue(application.MAPPER.DEFAULT_STRATEGY_MAPPER.containsKey(DefaultStrategyType.JPAAnnotation.getCode()));
        assertTrue(application.MAPPER.DEFAULT_STRATEGY_MAPPER.containsKey(DefaultStrategyType.MPAnnotation.getCode()));
        assertTrue(application.MAPPER.DEFAULT_STRATEGY_MAPPER.containsKey(DefaultStrategyType.HUMP_UNDERLINE.getCode()));
    }

    public void testStartupDoesNotOpenJdbcConnections() {
        DataPivotApplication application = DataPivotApplication.getInstance(getProject());
        assertTrue("startup must not populate the leftover DriverManager connection map",
                application.MAPPER.DR_DATABASE_CONNECTION_MAPPER.isEmpty());
        com.data.pivot.plugin.tool.DatabaseUtil.closeConnections();
        assertTrue(application.MAPPER.DR_DATABASE_CONNECTION_MAPPER.isEmpty());
    }

    public void testPluginDescriptorDeclaresDatabaseAndStartupContracts() throws Exception {
        Document pluginXml = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new File("src/main/resources/META-INF/plugin.xml"));

        assertElementText(pluginXml, "depends", "com.intellij.database");
        assertAttribute(pluginXml, "postStartupActivity", "implementation",
                "com.data.pivot.plugin.config.DataPivotInitializer");
        assertAttribute(pluginXml, "projectService", "serviceImplementation",
                "com.data.pivot.plugin.context.DataPivotApplication");
        assertAttribute(pluginXml, "projectConfigurable", "instance",
                "com.data.pivot.plugin.view.setting.DataPivotMappingSettingView");
        assertAttribute(pluginXml, "notificationGroup", "id", "Data Pivot Messages");
        assertAttribute(pluginXml, "idea-plugin", "require-restart", "true");
        assertAction(pluginXml, "DataPivot.Query", "com.data.pivot.plugin.actions.DataPivotQueryAction", "alt Q");
        assertAction(pluginXml, "DataPivot.Analysis", "com.data.pivot.plugin.actions.DataPivotAnalysisAction", "alt A");
        assertAction(pluginXml, "DataPivot.DataPivotORM", "com.data.pivot.plugin.actions.ORMNavigationAction", "alt R");
        assertAction(pluginXml, "DataPivot.DataPivotROM", "com.data.pivot.plugin.actions.ROMNavigationAction", "alt O");
        assertElementText(pluginXml, "resource-bundle", "messages.DataPivotBundle");
    }

    private static void assertElementText(Document document, String tagName, String expectedText) {
        NodeList elements = document.getElementsByTagName(tagName);
        for (int i = 0; i < elements.getLength(); i++) {
            if (expectedText.equals(elements.item(i).getTextContent().trim())) {
                return;
            }
        }
        fail("Missing <" + tagName + "> text: " + expectedText);
    }

    private static void assertAttribute(Document document, String tagName, String attributeName, String expectedValue) {
        NodeList elements = document.getElementsByTagName(tagName);
        for (int i = 0; i < elements.getLength(); i++) {
            if (expectedValue.equals(elements.item(i).getAttributes().getNamedItem(attributeName).getNodeValue())) {
                return;
            }
        }
        fail("Missing <" + tagName + "> " + attributeName + ": " + expectedValue);
    }

    private static void assertAction(Document document, String actionId, String className, String shortcut) {
        NodeList actions = document.getElementsByTagName("action");
        for (int i = 0; i < actions.getLength(); i++) {
            var action = actions.item(i).getAttributes();
            if (!actionId.equals(action.getNamedItem("id").getNodeValue())) {
                continue;
            }
            assertEquals(className, action.getNamedItem("class").getNodeValue());
            NodeList shortcuts = actions.item(i).getChildNodes();
            for (int j = 0; j < shortcuts.getLength(); j++) {
                if (!"keyboard-shortcut".equals(shortcuts.item(j).getNodeName())) {
                    continue;
                }
                assertEquals(shortcut, shortcuts.item(j).getAttributes().getNamedItem("first-keystroke").getNodeValue());
                return;
            }
            fail("Missing keyboard-shortcut for action " + actionId);
        }
        fail("Missing action " + actionId);
    }
}
