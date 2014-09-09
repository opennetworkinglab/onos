package org.onlab.onos.event;

/**
 * Entity capable of receiving events.
 */
public interface EventListener<E extends Event> {

    /**
     * Reacts to the specified event.
     *
     * @param event event to be processed
     */
    void event(E event);

}
