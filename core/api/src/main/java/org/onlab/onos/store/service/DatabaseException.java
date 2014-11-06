package org.onlab.onos.store.service;

/**
 * Base exception type for database failures.
 */
@SuppressWarnings("serial")
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable t) {
        super(message, t);
    }

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(Throwable t) {
        super(t);
    }

    public DatabaseException() {
    };
}