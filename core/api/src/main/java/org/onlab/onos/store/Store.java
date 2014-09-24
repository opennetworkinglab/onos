package org.onlab.onos.store;

import org.onlab.onos.event.Event;

/**
 * Abstraction of a entity capable of storing and/or distributing information
 * across a cluster.
 */
public interface Store<E extends Event, D extends StoreDelegate<E>> {

    /**
     * Sets the delegate on the store.
     *
     * @param delegate new store delegate
     * @throws java.lang.IllegalStateException if a delegate is already
     *                                         currently set on the store and is a different one that
     */
    void setDelegate(D delegate);

    /**
     * Withdraws the delegate from the store.
     *
     * @param delegate store delegate to withdraw
     * @throws java.lang.IllegalArgumentException if the delegate is not
     *                                            currently set on the store
     */
    void unsetDelegate(D delegate);

    /**
     * Indicates whether the store has a delegate.
     *
     * @return true if delegate is set
     */
    boolean hasDelegate();

}
