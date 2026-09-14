package com.data.pivot.plugin.tool;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringConverterTest {
    @Test
    public void toUnderScoreConvertsCamelCase() {
        assertEquals("hello_world_test", StringConverter.toUnderScore("helloWorldTest"));
    }

    @Test
    public void camelCaseConvertersKeepCurrentReplacementBehavior() {
        assertEquals("Helloworldtest", StringConverter.toBigCamelCase("hello_world_test"));
        assertEquals("helloworldtest", StringConverter.toCamelCase("hello_world_test"));
    }
}
