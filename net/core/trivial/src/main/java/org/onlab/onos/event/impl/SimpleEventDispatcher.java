package org.onlab.onos.event.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.event.Event;
import org.onlab.onos.event.EventDispatchService;
import org.onlab.onos.event.EventSink;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple implementation of an event dispatching service.
 */
@Component(immediate = true)
@Service
public class SimpleEventDispatcher implements EventDispatchService {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void post(Event event) {

    }

    @Override
    public <E extends Event> void addSink(Class<E> eventClass, EventSink<E> sink) {

    }

    @Override
    public <E extends Event> void removeSink(Class<E> eventClass) {

    }

    @Override
    public <E extends Event> EventSink<E> getSink(Class<E> eventClass) {
        return null;
    }

    @Override
    public Set<Class<? extends Event>> getSinks() {
        return null;
    }
}
