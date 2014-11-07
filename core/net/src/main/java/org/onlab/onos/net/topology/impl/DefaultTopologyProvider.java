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
package org.onlab.onos.net.topology.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.event.AbstractEventAccumulator;
import org.onlab.onos.event.Event;
import org.onlab.onos.event.EventAccumulator;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.link.LinkEvent;
import org.onlab.onos.net.link.LinkListener;
import org.onlab.onos.net.link.LinkService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.DefaultGraphDescription;
import org.onlab.onos.net.topology.GraphDescription;
import org.onlab.onos.net.topology.TopologyProvider;
import org.onlab.onos.net.topology.TopologyProviderRegistry;
import org.onlab.onos.net.topology.TopologyProviderService;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.onlab.onos.net.device.DeviceEvent.Type.*;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Default implementation of a network topology provider that feeds off
 * device and link subsystem events to trigger assembly and computation of
 * new topology snapshots.
 */
@Component(immediate = true)
@Service
public class DefaultTopologyProvider extends AbstractProvider
        implements TopologyProvider {

    // TODO: make these configurable
    private static final int MAX_EVENTS = 100;
    private static final int MAX_IDLE_MS = 5;
    private static final int MAX_BATCH_MS = 50;
    private static final int MAX_THREADS = 8;

    // FIXME: Replace with a system-wide timer instance;
    // TODO: Convert to use HashedWheelTimer or produce a variant of that; then decide which we want to adopt
    private static final Timer TIMER = new Timer();

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    private volatile boolean isStarted = false;

    private TopologyProviderService providerService;
    private DeviceListener deviceListener = new InternalDeviceListener();
    private LinkListener linkListener = new InternalLinkListener();

    private EventAccumulator accumulator;
    private ExecutorService executor;

    /**
     * Creates a provider with the supplier identifier.
     */
    public DefaultTopologyProvider() {
        super(new ProviderId("core", "org.onlab.onos.provider.topology"));
    }

    @Activate
    public synchronized void activate() {
        executor = newFixedThreadPool(MAX_THREADS, namedThreads("topo-build-%d"));
        accumulator = new TopologyChangeAccumulator();

        providerService = providerRegistry.register(this);
        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);

        isStarted = true;
        triggerRecompute();
        log.info("Started");
    }

    @Deactivate
    public synchronized void deactivate() {
        isStarted = false;

        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);
        providerRegistry.unregister(this);
        providerService = null;

        executor.shutdownNow();
        executor = null;

        log.info("Stopped");
    }

    @Override
    public void triggerRecompute() {
        triggerTopologyBuild(Collections.<Event>emptyList());
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
                                                deviceService.getDevices(),
                                                linkService.getLinks());
            providerService.topologyChanged(desc, reasons);
        }
    }

    // Callback for device events
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            DeviceEvent.Type type = event.type();
            if (type == DEVICE_ADDED || type == DEVICE_REMOVED ||
                    type == DEVICE_AVAILABILITY_CHANGED) {
                accumulator.add(event);
            }
        }
    }

    // Callback for link events
    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            accumulator.add(event);
        }
    }

    // Event accumulator for paced triggering of topology assembly.
    private class TopologyChangeAccumulator
            extends AbstractEventAccumulator implements EventAccumulator {

        TopologyChangeAccumulator() {
            super(TIMER, MAX_EVENTS, MAX_BATCH_MS, MAX_IDLE_MS);
        }

        @Override
        public void processEvents(List<Event> events) {
            triggerTopologyBuild(events);
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
                log.warn("Unable to compute topology due to: {}", e.getMessage());
            }
        }
    }

}
