package org.onlab.onos.event;

import java.util.Set;

/**
 * Abstraction of an event sink broker capable of tracking sinks based on
 * their event class.
 */
public interface EventSinkBroker {

    /**
     * Adds the specified sink for the given event class.
     *
     * @param eventClass event class
     * @param sink       event sink
     * @param <E>        type of event
     */
    <E extends Event> void addSink(Class<E> eventClass, EventSink<E> sink);

    /**
     * Removes the sink associated with the given event class.
     *
     * @param eventClass event class
     * @param <E>        type of event
     */
    <E extends Event> void removeSink(Class<E> eventClass);

    /**
     * Returns the event sink associated with the specified event class.
     *
     * @param eventClass event class
     * @param <E>        type of event
     * @return event sink or null if none found
     */
    <E extends Event> EventSink<E> getSink(Class<E> eventClass);

    /**
     * Returns the set of all event classes for which sinks are presently
     * registered.
     *
     * @return set of event classes
     */
    Set<Class<? extends Event>> getSinks();

}
