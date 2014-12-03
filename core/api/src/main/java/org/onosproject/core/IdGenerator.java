package org.onlab.onos.core;

/**
 * A generalized interface for ID generation
 *
 * {@link #getNewId()} generates a globally unique ID instance on
 * each invocation.
 */
public interface IdGenerator {
    /**
     * Returns a globally unique ID instance.
     *
     * @return globally unique ID instance
     */
    long getNewId();
}
