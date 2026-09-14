package com.data.pivot.plugin.tool;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BackgroundQuerySupportTest {
    @Test
    public void executeSurfacesQueryFailedExceptionWithoutThrowing() {
        AtomicReference<BackgroundQuerySupport.Result> seen = new AtomicReference<>();
        BackgroundQuerySupport.execute(null, "query", () -> {
            throw new QueryFailedException("boom");
        }, seen::set);

        assertTrue(seen.get().isError());
        assertEquals("boom", seen.get().getError());
        assertTrue(seen.get().getRows().isEmpty());
    }

    @Test
    public void executeReturnsRowsOnSuccess() {
        AtomicReference<BackgroundQuerySupport.Result> seen = new AtomicReference<>();
        BackgroundQuerySupport.execute(null, "query", List::of, seen::set);

        assertTrue(!seen.get().isError());
        assertEquals(0, seen.get().getRows().size());
    }
}
