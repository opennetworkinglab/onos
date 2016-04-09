/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.events;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onlab.util.Tools.minPriority;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.UnmodifiableDeque;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.Event;
import org.onosproject.event.ListenerTracker;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application to store history of instance local ONOS Events.
 */
@Component(immediate = true)
@Service
public class EventHistoryManager
    implements EventHistoryService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Property(name = "excludeStatsEvent", boolValue = true,
              label = "Exclude stats related events")
    private boolean excludeStatsEvent = true;

    @Property(name = "sizeLimit", intValue = 10_000,
              label = "Number of event history to store")
    private int sizeLimit = 10_000;

    private ApplicationId appId;

    private ListenerTracker listeners;

    // Using Deque so that it'll be possible to iterate from both ends
    // (Tail-end is the most recent event)
    private final Deque<Event<?, ?>> history = new ConcurrentLinkedDeque<>();

    private ScheduledExecutorService pruner;

    // pruneEventHistoryTask() execution interval in seconds
    private long pruneInterval = 5;


    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onosproject.events");
        log.debug("Registered as {}", appId);

        pruner = newSingleThreadScheduledExecutor(
                  minPriority(groupedThreads("onos/events", "history-pruner", log)));

        pruner.scheduleWithFixedDelay(this::pruneEventHistoryTask,
                                      pruneInterval, pruneInterval, TimeUnit.SECONDS);

        listeners = new ListenerTracker();
        listeners.addListener(mastershipService, new InternalMastershipListener())
                 .addListener(deviceService, new InternalDeviceListener())
                 .addListener(linkService, new InternalLinkListener())
                 .addListener(topologyService, new InternalTopologyListener())
                 .addListener(hostService, new InternalHostListener())
                 .addListener(clusterService, new InternalClusterListener());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        listeners.removeListeners();

        pruner.shutdownNow();
        history.clear();

        log.info("Stopped");
    }

    @Override
    public Deque<Event<?, ?>> history() {
        return UnmodifiableDeque.unmodifiableDeque(history);
    }

    @Override
    public void clear() {
        history.clear();
    }

    // This method assumes only 1 call is in flight at the same time.
    private void pruneEventHistoryTask() {
        int size = history.size();
        int overflows = size - sizeLimit;
        if (overflows > 0) {
            for (int i = 0; i < overflows; ++i) {
                history.poll();
            }
        }
    }

    private void addEvent(Event<?, ?> event) {
        history.offer(event);
    }

    class InternalMastershipListener
            implements MastershipListener {

        @Override
        public void event(MastershipEvent event) {
            addEvent(event);
        }
    }

    class InternalDeviceListener
            implements DeviceListener {

        @Override
        public boolean isRelevant(DeviceEvent event) {
            if (excludeStatsEvent) {
                return event.type() != DeviceEvent.Type.PORT_STATS_UPDATED;
            } else {
                return true;
            }
        }

        @Override
        public void event(DeviceEvent event) {
            addEvent(event);
        }
    }

    class InternalLinkListener
            implements LinkListener {

        @Override
        public void event(LinkEvent event) {
            addEvent(event);
        }
    }

    class InternalTopologyListener
            implements TopologyListener {

        @Override
        public void event(TopologyEvent event) {
            addEvent(event);
        }
    }

    class InternalHostListener
            implements HostListener {

        @Override
        public void event(HostEvent event) {
            addEvent(event);
        }
    }

    class InternalClusterListener
            implements ClusterEventListener {

        @Override
        public void event(ClusterEvent event) {
            addEvent(event);
        }
    }

}
