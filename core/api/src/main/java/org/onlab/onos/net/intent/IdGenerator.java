package org.onlab.onos.net.intent;
//TODO is this the right package?

/**
 * A generalized interface for ID generation
 *
 * {@link #getNewId()} generates a globally unique ID instance on
 * each invocation.
 *
 * @param <T> the type of ID
 */
// TODO: do we need to define a base marker interface for ID,
// then changed the type parameter to <T extends BaseId> something
// like that?
public interface IdGenerator<T> {
    /**
     * Returns a globally unique ID instance.
     *
     * @return globally unique ID instance
     */
    T getNewId();
}
