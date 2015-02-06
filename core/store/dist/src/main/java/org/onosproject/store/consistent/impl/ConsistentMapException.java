package org.onosproject.store.consistent.impl;

/**
 * Top level exception for ConsistentMap failures.
 */
@SuppressWarnings("serial")
public class ConsistentMapException extends RuntimeException {
    public ConsistentMapException() {
    }

    public ConsistentMapException(Throwable t) {
        super(t);
    }

    /**
     * ConsistentMap operation timeout.
     */
    public static class Timeout extends ConsistentMapException {
    }

    /**
     * ConsistentMap operation interrupted.
     */
    public static class Interrupted extends ConsistentMapException {
    }
}
