package com.data.pivot.plugin.view.query;

import com.data.pivot.plugin.entity.DatabaseQueryConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Abstraction over {@link com.data.pivot.plugin.tool.QueryTool} so Query UI can be tested without a live database.
 */
@FunctionalInterface
public interface QueryRunner {
    @Nullable
    List<Map<String, Object>> query(@NotNull DatabaseQueryConfig config);
}
