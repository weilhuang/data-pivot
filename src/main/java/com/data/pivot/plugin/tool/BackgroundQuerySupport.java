package com.data.pivot.plugin.tool;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runs JDBC work off the EDT. Unit/headless tests stay synchronous so injected
 * {@link com.data.pivot.plugin.view.query.QueryRunner}s remain immediately assertable.
 */
public final class BackgroundQuerySupport {
    private BackgroundQuerySupport() {
    }

    public static void execute(
            @Nullable Project project,
            @NotNull String title,
            @NotNull Supplier<List<Map<String, Object>>> query,
            @NotNull Consumer<Result> onEdt
    ) {
        Application application = ApplicationManager.getApplication();
        if (application == null || application.isUnitTestMode() || application.isHeadlessEnvironment()) {
            onEdt.accept(invoke(query));
            return;
        }
        ProgressManager.getInstance().run(new Task.Backgroundable(project, title, true) {
            private Result result = Result.empty();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                result = invoke(query);
            }

            @Override
            public void onSuccess() {
                onEdt.accept(result);
            }

            @Override
            public void onCancel() {
                onEdt.accept(Result.cancelled());
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                onEdt.accept(Result.error(error.getMessage() == null ? error.toString() : error.getMessage()));
            }
        });
    }

    private static Result invoke(@NotNull Supplier<List<Map<String, Object>>> query) {
        try {
            List<Map<String, Object>> rows = query.get();
            return Result.success(rows == null ? List.of() : rows);
        } catch (QueryFailedException failed) {
            return Result.error(failed.getMessage());
        } catch (RuntimeException runtime) {
            Throwable cause = runtime.getCause();
            if (cause instanceof QueryFailedException failed) {
                return Result.error(failed.getMessage());
            }
            return Result.error(runtime.getMessage() == null ? runtime.toString() : runtime.getMessage());
        }
    }

    public static final class Result {
        private final List<Map<String, Object>> rows;
        private final String error;
        private final boolean cancelled;

        private Result(List<Map<String, Object>> rows, String error, boolean cancelled) {
            this.rows = rows;
            this.error = error;
            this.cancelled = cancelled;
        }

        public static Result success(@NotNull List<Map<String, Object>> rows) {
            return new Result(List.copyOf(rows), null, false);
        }

        public static Result error(@Nullable String message) {
            return new Result(List.of(), message == null ? "" : message, false);
        }

        public static Result cancelled() {
            return new Result(List.of(), null, true);
        }

        public static Result empty() {
            return success(Collections.emptyList());
        }

        public @NotNull List<Map<String, Object>> getRows() {
            return rows;
        }

        public @Nullable String getError() {
            return error;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public boolean isError() {
            return error != null;
        }
    }
}
