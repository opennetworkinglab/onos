package org.onlab.onos.event.impl;

import org.onlab.onos.event.DefaultEventSinkRegistry;
import org.onlab.onos.event.Event;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.event.EventSink;

import static com.google.common.base.Preconditions.checkState;

/**
 * Implements event delivery system that delivers events synchronously, or
 * in-line with the post method invocation.
 */
public class TestEventDispatcher extends DefaultEventSinkRegistry
        implements EventDeliveryService {

    @Override
    @SuppressWarnings("unchecked")
    public void post(Event event) {
        EventSink sink = getSink(event.getClass());
        checkState(sink != null, "No sink for event %s", event);
        sink.process(event);
    }

}
