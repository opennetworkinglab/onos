package org.onlab.onos.event;

import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base implementation of event sink broker.
 */
public class AbstractEventSinkBroker implements EventSinkBroker {

    private final Map<Class<? extends Event>, EventSink<? extends Event>> sinks =
            new ConcurrentHashMap<>();

    @Override
    public <E extends Event> void addSink(Class<E> eventClass, EventSink<E> sink) {
        checkNotNull(eventClass, "Event class cannot be null");
        checkNotNull(sink, "Event sink cannot be null");
        checkArgument(!sinks.containsKey(eventClass),
                      "Event sink already registered for %s", eventClass.getName());
        sinks.put(eventClass, sink);
    }

    @Override
    public <E extends Event> void removeSink(Class<E> eventClass) {
        checkNotNull(eventClass, "Event class cannot be null");
        checkArgument(sinks.remove(eventClass) != null,
                      "Event sink not registered for %s", eventClass.getName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Event> EventSink<E> getSink(Class<E> eventClass) {
        return (EventSink<E>) sinks.get(eventClass);
    }

    @Override
    public Set<Class<? extends Event>> getSinks() {
        return ImmutableSet.copyOf(sinks.keySet());
    }
}
