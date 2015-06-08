package org.onosproject.store.service;

/**
 * Listener to be notified about updates to a DistributedSet.
 */
public interface SetEventListener<E> {
    /**
     * Reacts to the specified event.
     *
     * @param event the event
     */
    void event(SetEvent<E> event);
}
