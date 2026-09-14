package com.data.pivot.plugin.tool;

/**
 * Thrown when a JDBC query fails. Callers must not show modal dialogs from this type;
 * surface the message in a status bar or notification on the EDT.
 */
public class QueryFailedException extends RuntimeException {
    public QueryFailedException(String message) {
        super(message);
    }

    public QueryFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
