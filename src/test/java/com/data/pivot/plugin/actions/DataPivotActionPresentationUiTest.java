package com.data.pivot.plugin.actions;

import com.data.pivot.plugin.DataPivotPlatformTestCase;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.testFramework.JUnit38AssumeSupportRunner;
import com.intellij.testFramework.TestActionEvent;
import org.junit.runner.RunWith;

@RunWith(JUnit38AssumeSupportRunner.class)
public class DataPivotActionPresentationUiTest extends DataPivotPlatformTestCase {
    public void testQueryActionIsDisabledWithoutPsiField() {
        assertDisabled(new DataPivotQueryAction());
    }

    public void testAnalysisActionIsDisabledWithoutPsiField() {
        assertDisabled(new DataPivotAnalysisAction());
    }

    public void testOrmActionIsDisabledWithoutPsiField() {
        assertDisabled(new ORMNavigationAction());
    }

    public void testRomActionIsDisabledWithoutDatabaseColumn() {
        assertDisabled(new ROMNavigationAction());
    }

    private static void assertDisabled(AnAction action) {
        AnActionEvent event = TestActionEvent.createTestEvent(action);
        action.update(event);
        Presentation presentation = event.getPresentation();
        assertFalse(presentation.isEnabled());
    }
}
