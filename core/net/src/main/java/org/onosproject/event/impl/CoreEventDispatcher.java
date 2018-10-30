/*
 * Copyright 2014-present Open Networking Foundation
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

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.onlab.util.SharedExecutors;
import org.onosproject.event.AbstractEvent;
import org.onosproject.event.DefaultEventSinkRegistry;
import org.onosproject.event.Event;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.EventSink;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.topology.TopologyEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.EVENT_READ;
import static org.onosproject.security.AppPermission.Type.EVENT_WRITE;
import static org.slf4j.LoggerFactory.getLogger;
/**
 * Simple implementation of an event dispatching service.
 */
@Component(immediate = true, service = EventDeliveryService.class)
public class CoreEventDispatcher extends DefaultEventSinkRegistry
        implements EventDeliveryService {

    private final Logger log = getLogger(getClass());


    private DispatchLoop topologyDispatcher = new DispatchLoop("topology");
    private DispatchLoop programmingDispatcher = new DispatchLoop("programming");
    private DispatchLoop defaultDispatcher = new DispatchLoop("default");

    private Map<Class, DispatchLoop> dispatcherMap =
            new ImmutableMap.Builder<Class, DispatchLoop>()
                .put(TopologyEvent.class, topologyDispatcher)
                .put(DeviceEvent.class, topologyDispatcher)
                .put(LinkEvent.class, topologyDispatcher)
                .put(HostEvent.class, topologyDispatcher)
                .put(FlowRuleEvent.class, programmingDispatcher)
                .put(IntentEvent.class, programmingDispatcher)
                .build();

    private Set<DispatchLoop> dispatchers =
            new ImmutableSet.Builder<DispatchLoop>()
                .addAll(dispatcherMap.values())
                .add(defaultDispatcher)
                .build();

    // Default number of millis a sink can take to process an event.
    private static final long DEFAULT_EXECUTE_MS = 5_000; // ms
    private static final long WATCHDOG_MS = 250; // ms

    @SuppressWarnings("unchecked")
    private static final Event KILL_PILL = new AbstractEvent(null, 0) {
    };

    private long maxProcessMillis = DEFAULT_EXECUTE_MS;

    private DispatchLoop getDispatcher(Event event) {
        DispatchLoop dispatcher = dispatcherMap.get(event.getClass());
        if (dispatcher == null) {
            dispatcher = defaultDispatcher;
        }
        return dispatcher;
    }

    @Override
    public void post(Event event) {

        if (!getDispatcher(event).add(event)) {
            log.error("Unable to post event {}", event);
        }
    }

    @Activate
    public void activate() {

        if (maxProcessMillis != 0) {
            dispatchers.forEach(DispatchLoop::start);
        }

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        dispatchers.forEach(DispatchLoop::stop);

        log.info("Stopped");
    }

    @Override
    public void setDispatchTimeLimit(long millis) {
        checkPermission(EVENT_WRITE);
        checkArgument(millis == 0 || millis >= WATCHDOG_MS,
                      "Time limit must be greater than %s", WATCHDOG_MS);
        long oldMillis = maxProcessMillis;
        maxProcessMillis = millis;

        if (millis == 0 && oldMillis != 0) {
            dispatchers.forEach(DispatchLoop::stopWatchdog);
        } else if (millis != 0 && oldMillis == 0) {
            dispatchers.forEach(DispatchLoop::startWatchdog);
        }
    }

    @Override
    public long getDispatchTimeLimit() {
        checkPermission(EVENT_READ);
        return maxProcessMillis;
    }

    // Auxiliary event dispatching loop that feeds off the events queue.
    private class DispatchLoop implements Runnable {
        private final String name;
        private volatile boolean stopped;
        private volatile EventSink lastSink;
        // Means to detect long-running sinks
        private final Stopwatch stopwatch = Stopwatch.createUnstarted();
        private TimerTask watchdog;
        private volatile Future<?> dispatchFuture;
        private final BlockingQueue<Event> eventsQueue;
        private final ExecutorService executor;

        DispatchLoop(String name) {
            this.name = name;
            executor = newSingleThreadExecutor(
                    groupedThreads("onos/event",
                    "dispatch-" + name + "%d", log));
            eventsQueue = new LinkedBlockingQueue<>();
        }

        public boolean add(Event event) {
            return eventsQueue.add(event);
        }

        @Override
        public void run() {
            log.info("Dispatch loop({}) initiated", name);
            while (!stopped) {
                try {
                    // Fetch the next event and if it is the kill-pill, bail
                    Event event = eventsQueue.take();
                    if (event != KILL_PILL) {
                        process(event);
                    }
                } catch (InterruptedException e) {
                    log.warn("Dispatch loop interrupted");
                } catch (Exception | Error e) {
                    log.warn("Error encountered while dispatching event:", e);
                }
            }
            log.info("Dispatch loop({}) terminated", name);
        }

        // Locate the sink for the event class and use it to process the event
        @SuppressWarnings("unchecked")
        private void process(Event event) {
            EventSink sink = getSink(event.getClass());
            if (sink != null) {
                lastSink = sink;
                stopwatch.start();
                sink.process(event);
                stopwatch.reset();
            } else {
                log.warn("No sink registered for event class {}",
                         event.getClass().getName());
            }
        }

        void stop() {
            stopped = true;
            add(KILL_PILL);
            if (null != dispatchFuture) {
                dispatchFuture.cancel(true);
            }
            stopWatchdog();
        }

        void start() {
            stopped = false;
            dispatchFuture = executor.submit(this);
            startWatchdog();
        }

        // Monitors event sinks to make sure none take too long to execute.
        private class Watchdog extends TimerTask {
            @Override
            public void run() {
                long elapsedTimeMillis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
                if (elapsedTimeMillis > maxProcessMillis) {
                    stopwatch.reset();
                    log.warn("Event sink {} exceeded execution time limit: {} ms; " +
                             "spawning new dispatch loop",
                             lastSink.getClass().getName(), elapsedTimeMillis);

                    // Notify the sink that it has exceeded its time limit.
                    lastSink.onProcessLimit();

                    // Cancel the old dispatch loop and submit a new one.

                    stop();
                    start();
                }
            }
        }

        private void startWatchdog() {
            log.info("Starting watchdog task for dispatcher {}", name);
            watchdog = new Watchdog();
            SharedExecutors.getTimer().schedule(watchdog, WATCHDOG_MS, WATCHDOG_MS);
        }

        private void stopWatchdog() {
            log.info("Stopping watchdog task for dispatcher {}", name);
            if (watchdog != null) {
                watchdog.cancel();
            }
        }
    }


}
