package org.onosproject.store.service;

/**
 * Listener to be notified about updates to a ConsitentMap.
 */
public interface MapEventListener<K, V> {
    /**
     * Reacts to the specified event.
     *
     * @param event the event
     */
    void event(MapEvent<K, V> event);
}
