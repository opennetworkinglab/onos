package org.onlab.onos.event;

/**
 * Abstraction of a mechanism capable of accepting and dispatching events to
 * appropriate event sinks. Where the event sinks are obtained is unspecified.
 * Similarly, whether the events are accepted and dispatched synchronously
 * or asynchronously is unspecified as well.
 */
public interface EventDispatcher<E extends Event> {

    /**
     * Posts the specified event for dispatching.
     *
     * @param event event to be posted
     */
    void post(E event);

}
