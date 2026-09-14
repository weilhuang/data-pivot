package com.data.pivot.plugin.view.report;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnalysisResultModelTest {
    @Test
    public void fromMapsReadsValueCountAndPercentage() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("status", "ACTIVE");
        row.put("rs_count", 18);
        row.put("percentage", 90.0);

        List<AnalysisRow> rows = AnalysisResultModel.fromMaps(List.of(row), "status");

        assertEquals(1, rows.size());
        assertEquals("ACTIVE", rows.get(0).getValue());
        assertEquals("18", rows.get(0).getCount());
        assertEquals("90.0", rows.get(0).getPercentage());
    }

    @Test
    public void fromMapsReturnsEmptyForMissingInput() {
        assertTrue(AnalysisResultModel.fromMaps(null, "status").isEmpty());
        assertTrue(AnalysisResultModel.fromMaps(List.of(), "status").isEmpty());
        assertTrue(AnalysisResultModel.fromMaps(List.of(Map.of("status", "A")), null).isEmpty());
        assertTrue(AnalysisResultModel.fromMaps(List.of(Map.of("status", "A")), "  ").isEmpty());
    }

    @Test
    public void fromMapsSkipsNullRowsAndStringifiesNullCells() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("status", null);
        row.put("rs_count", null);
        row.put("percentage", 1);

        List<AnalysisRow> rows = AnalysisResultModel.fromMaps(java.util.Arrays.asList(null, row), "status");

        assertEquals(1, rows.size());
        assertEquals("", rows.get(0).getValue());
        assertEquals("", rows.get(0).getCount());
        assertEquals("1", rows.get(0).getPercentage());
    }
}
