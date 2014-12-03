/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.event.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.AbstractEvent;
import org.onosproject.event.DefaultEventSinkRegistry;
import org.onosproject.event.Event;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.EventSink;
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
