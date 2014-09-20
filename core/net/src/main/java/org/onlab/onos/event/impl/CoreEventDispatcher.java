package org.onlab.onos.event.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.event.AbstractEvent;
import org.onlab.onos.event.DefaultEventSinkRegistry;
import org.onlab.onos.event.Event;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.event.EventSink;
import org.slf4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Simple implementation of an event dispatching service.
 */
@Component(immediate = true)
@Service
public class CoreEventDispatcher extends DefaultEventSinkRegistry
        implements EventDeliveryService {

    private final Logger log = getLogger(getClass());

    private final ExecutorService executor =
            newSingleThreadExecutor(namedThreads("event-dispatch-%d"));

    @SuppressWarnings("unchecked")
    private static final Event KILL_PILL = new AbstractEvent(null, 0) {
    };

    private final BlockingQueue<Event> events = new LinkedBlockingQueue<>();

    private volatile boolean stopped = false;

    @Override
    public void post(Event event) {
        events.add(event);
    }

    @Activate
    public void activate() {
        stopped = false;
        executor.execute(new DispatchLoop());
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        stopped = true;
        post(KILL_PILL);
        log.info("Stopped");
    }

    // Auxiliary event dispatching loop that feeds off the events queue.
    private class DispatchLoop implements Runnable {
        @Override
        @SuppressWarnings("unchecked")
        public void run() {
            log.info("Dispatch loop initiated");
            while (!stopped) {
                try {
                    // Fetch the next event and if it is the kill-pill, bail
                    Event event = events.take();
                    if (event == KILL_PILL) {
                        break;
                    }

                    // Locate the sink for the event class and use it to
                    // process the event
                    EventSink sink = getSink(event.getClass());
                    if (sink != null) {
                        sink.process(event);
                    } else {
                        log.warn("No sink registered for event class {}",
                                 event.getClass());
                    }
                } catch (Exception e) {
                    log.warn("Error encountered while dispatching event:", e);
                }
            }
            log.info("Dispatch loop terminated");
        }
    }

}
