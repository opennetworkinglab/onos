package org.onlab.onos.event;

/**
 * Abstraction of a mechanism capable of accepting and dispatching events.
 * Whether the events are accepted and the dispatched synchronously or
 * asynchronously is unspecified.
 */
public interface EventDispatcher<E extends Event> {

    /**
     * Posts the specified event for dispatching.
     *
     * @param event event to be posted
     */
    void post(E event);

}
