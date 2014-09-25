package org.onlab.onos.store;

import org.onlab.onos.event.Event;

/**
 * Entity associated with a store and capable of receiving notifications of
 * events within the store.
 */
public interface StoreDelegate<E extends Event> {

    /**
     * Notifies the delegate via the specified event.
     *
     * @param event store generated event
     */
    void notify(E event);

}
