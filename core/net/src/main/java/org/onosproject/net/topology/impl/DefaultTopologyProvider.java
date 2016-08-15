/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.topology.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.core.CoreService.CORE_PROVIDER_ID;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_REMOVED;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutorService;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.AbstractAccumulator;
import org.onlab.util.Accumulator;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.event.Event;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.topology.DefaultGraphDescription;
import org.onosproject.net.topology.GraphDescription;
import org.onosproject.net.topology.TopologyProvider;
import org.onosproject.net.topology.TopologyProviderRegistry;
import org.onosproject.net.topology.TopologyProviderService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

/**
 * Default implementation of a network topology provider that feeds off
 * device and link subsystem events to trigger assembly and computation of
 * new topology snapshots.
 */
@Component(immediate = true)
@Service
public class DefaultTopologyProvider extends AbstractProvider
        implements TopologyProvider {

    private static final int MAX_THREADS = 8;
    private static final int DEFAULT_MAX_EVENTS = 1000;
    private static final int DEFAULT_MAX_IDLE_MS = 10;
    private static final int DEFAULT_MAX_BATCH_MS = 50;

    // FIXME: Replace with a system-wide timer instance;
    // TODO: Convert to use HashedWheelTimer or produce a variant of that; then decide which we want to adopt
    private static final Timer TIMER = new Timer("onos-topo-event-batching");

    @Property(name = "maxEvents", intValue = DEFAULT_MAX_EVENTS,
            label = "Maximum number of events to accumulate")
    private int maxEvents = DEFAULT_MAX_EVENTS;

    @Property(name = "maxIdleMs", intValue = DEFAULT_MAX_IDLE_MS,
            label = "Maximum number of millis between events")
    private int maxIdleMs = DEFAULT_MAX_IDLE_MS;

    @Property(name = "maxBatchMs", intValue = DEFAULT_MAX_BATCH_MS,
            label = "Maximum number of millis for whole batch")
    private int maxBatchMs = DEFAULT_MAX_BATCH_MS;

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private volatile boolean isStarted = false;

    private TopologyProviderService providerService;
    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final LinkListener linkListener = new InternalLinkListener();

    private Accumulator<Event> accumulator;
    private ExecutorService executor;

    /**
     * Creates a provider with the supplier identifier.
     */
    public DefaultTopologyProvider() {
        super(CORE_PROVIDER_ID);
    }

    @Activate
    public synchronized void activate(ComponentContext context) {
        cfgService.registerProperties(DefaultTopologyProvider.class);
        executor = newFixedThreadPool(MAX_THREADS, groupedThreads("onos/topo", "build-%d", log));
        accumulator = new TopologyChangeAccumulator();
        logConfig("Configured");

        modified(context);

        providerService = providerRegistry.register(this);
        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);

        isStarted = true;
        triggerRecompute();
        log.info("Started");
    }

    @Deactivate
    public synchronized void deactivate(ComponentContext context) {
        cfgService.unregisterProperties(DefaultTopologyProvider.class, false);
        isStarted = false;

        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);
        providerRegistry.unregister(this);
        providerService = null;

        executor.shutdownNow();
        executor = null;

        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        if (context == null) {
            accumulator = new TopologyChangeAccumulator();
            logConfig("Reconfigured");
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();
        int newMaxEvents, newMaxBatchMs, newMaxIdleMs;
        try {
            String s = get(properties, "maxEvents");
            newMaxEvents = isNullOrEmpty(s) ? maxEvents : Integer.parseInt(s.trim());

            s = get(properties, "maxBatchMs");
            newMaxBatchMs = isNullOrEmpty(s) ? maxBatchMs : Integer.parseInt(s.trim());

            s = get(properties, "maxIdleMs");
            newMaxIdleMs = isNullOrEmpty(s) ? maxIdleMs : Integer.parseInt(s.trim());

        } catch (NumberFormatException | ClassCastException e) {
            newMaxEvents = DEFAULT_MAX_EVENTS;
            newMaxBatchMs = DEFAULT_MAX_BATCH_MS;
            newMaxIdleMs = DEFAULT_MAX_IDLE_MS;
        }

        if (newMaxEvents != maxEvents || newMaxBatchMs != maxBatchMs || newMaxIdleMs != maxIdleMs) {
            maxEvents = newMaxEvents;
            maxBatchMs = newMaxBatchMs;
            maxIdleMs = newMaxIdleMs;
            accumulator = maxEvents > 1 ? new TopologyChangeAccumulator() : null;
            logConfig("Reconfigured");
        }
    }

    private void logConfig(String prefix) {
        log.info("{} with maxEvents = {}; maxBatchMs = {}; maxIdleMs = {}; accumulator={}",
                 prefix, maxEvents, maxBatchMs, maxIdleMs, accumulator != null);
    }


    @Override
    public void triggerRecompute() {
        triggerTopologyBuild(Collections.emptyList());
    }

    /**
     * Triggers assembly of topology data citing the specified events as the
     * reason.
     *
     * @param reasons events which triggered the topology change
     */
    private synchronized void triggerTopologyBuild(List<Event> reasons) {
        if (executor != null) {
            executor.execute(new TopologyBuilderTask(reasons));
        }
    }

    // Builds the topology using the latest device and link information
    // and citing the specified events as reasons for the change.
    private void buildTopology(List<Event> reasons) {
        if (isStarted) {
            GraphDescription desc =
                    new DefaultGraphDescription(System.nanoTime(),
                                                System.currentTimeMillis(),
                                                deviceService.getAvailableDevices(),
                                                linkService.getActiveLinks());
            providerService.topologyChanged(desc, reasons);
        }
    }

    private void processEvent(Event event) {
        if (accumulator != null) {
            accumulator.add(event);
        } else {
            triggerTopologyBuild(ImmutableList.of(event));
        }
    }

    // Callback for device events
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            DeviceEvent.Type type = event.type();
            if (type == DEVICE_ADDED || type == DEVICE_REMOVED ||
                    type == DEVICE_AVAILABILITY_CHANGED) {
                processEvent(event);
            }
        }
    }

    // Callback for link events
    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            processEvent(event);
        }
    }

    // Event accumulator for paced triggering of topology assembly.
    private class TopologyChangeAccumulator extends AbstractAccumulator<Event> {
        TopologyChangeAccumulator() {
            super(TIMER, maxEvents, maxBatchMs, maxIdleMs);
        }

        @Override
        public void processItems(List<Event> items) {
            triggerTopologyBuild(items);
        }
    }

    // Task for building topology data in a separate thread.
    private class TopologyBuilderTask implements Runnable {
        private final List<Event> reasons;

        public TopologyBuilderTask(List<Event> reasons) {
            this.reasons = reasons;
        }

        @Override
        public void run() {
            try {
                buildTopology(reasons);
            } catch (Exception e) {
                log.warn("Unable to compute topology", e);
            }
        }
    }

}
