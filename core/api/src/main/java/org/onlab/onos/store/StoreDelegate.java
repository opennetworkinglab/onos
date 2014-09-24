package org.onlab.onos.store;

import org.onlab.onos.event.Event;

/**
 * Entity associated with a store and capable of receiving notifications of
 * events within the store.
 */
public interface StoreDelegate<E extends Event> {

    void notify(E event);

}
