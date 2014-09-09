package org.onlab.onos.event;

/**
 * Abstraction of an event sink capable of processing the specified event types.
 */
public interface EventSink<E extends Event> {

    /**
     * Processes the specified event.
     *
     * @param event event to be processed
     */
    void process(E event);

}
