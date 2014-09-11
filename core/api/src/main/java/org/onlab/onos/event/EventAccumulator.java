package org.onlab.onos.event;

import java.util.List;

/**
 * Abstraction of an accumulator capable of collecting events and at some
 * point in time triggers processing of all previously accumulated events.
 */
public interface EventAccumulator {

    /**
     * Adds an event to the current batch. This operation may, or may not
     * trigger processing of the current batch of events.
     *
     * @param event event to be added to the current batch
     */
    void add(Event event);

    /**
     * Processes the specified list of accumulated events.
     *
     * @param events list of accumulated events
     */
    void processEvents(List<Event> events);

}
