package com.data.pivot.plugin.tool;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StringConverterTest {
    @Test
    public void toUnderScoreConvertsCamelCase() {
        assertEquals("hello_world_test", StringConverter.toUnderScore("helloWorldTest"));
        assertEquals("sys_user", StringConverter.toUnderScore("SysUser"));
        assertEquals("user_id", StringConverter.toUnderScore("userId"));
    }

    @Test
    public void toCamelCaseUppercasesCapturedGroups() {
        assertEquals("helloWorldTest", StringConverter.toCamelCase("hello_world_test"));
        assertEquals("userId", StringConverter.toCamelCase("user_id"));
        assertEquals("sysUser", StringConverter.toCamelCase("sys_user"));
    }

    @Test
    public void toBigCamelCaseUppercasesFirstLetterAndCapturedGroups() {
        assertEquals("HelloWorldTest", StringConverter.toBigCamelCase("hello_world_test"));
        assertEquals("UserId", StringConverter.toBigCamelCase("user_id"));
        assertEquals("SysUser", StringConverter.toBigCamelCase("sys_user"));
    }

    @Test
    public void allCapsSnakeColumnsBecomeCamelCase() {
        assertEquals("userId", StringConverter.toCamelCase("USER_ID"));
        assertEquals("UserId", StringConverter.toBigCamelCase("USER_ID"));
        assertEquals("createdAt", StringConverter.toCamelCase("CREATED_AT"));
        assertEquals("CreatedAt", StringConverter.toBigCamelCase("CREATED_AT"));
    }

    @Test
    public void consecutiveUnderscoresAreCollapsed() {
        assertEquals("helloWorld", StringConverter.toCamelCase("hello__world"));
        assertEquals("HelloWorld", StringConverter.toBigCamelCase("hello__world"));
    }

    @Test
    public void alreadyCamelIdentifiersWithoutUnderscoresStayStable() {
        assertEquals("userId", StringConverter.toCamelCase("userId"));
        assertEquals("UserId", StringConverter.toBigCamelCase("userId"));
    }

    @Test
    public void nullAndEmptyInputsAreReturnedAsIs() {
        assertNull(StringConverter.toCamelCase(null));
        assertNull(StringConverter.toBigCamelCase(null));
        assertNull(StringConverter.toUnderScore(null));
        assertEquals("", StringConverter.toCamelCase(""));
        assertEquals("", StringConverter.toBigCamelCase(""));
        assertEquals("", StringConverter.toUnderScore(""));
    }
}
