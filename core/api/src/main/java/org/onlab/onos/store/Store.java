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
     */
    void setDelegate(D delegate);

    /**
     * Get the current store delegate.
     *
     * @return store delegate
     */
    D getDelegate();

}
