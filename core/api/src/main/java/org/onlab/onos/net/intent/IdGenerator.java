package org.onlab.onos.net.intent;

/**
 * A generalized interface for ID generation
 * <p/>
 * {@link #getNewId()} generates a globally unique ID instance on
 * each invocation.
 *
 * @param <T> the type of ID
 */
@Deprecated
public interface IdGenerator<T> {
    /**
     * Returns a globally unique ID instance.
     *
     * @return globally unique ID instance
     */
    T getNewId();
}
