/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.impl.topo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static org.onosproject.cluster.ClusterEvent.Type.INSTANCE_ADDED;
import static org.onosproject.net.device.DeviceEvent.Type.DEVICE_ADDED;
import static org.onosproject.net.host.HostEvent.Type.HOST_ADDED;
import static org.onosproject.net.link.LinkEvent.Type.LINK_ADDED;
import static org.onosproject.ui.impl.topo.TopoUiEvent.Type.SUMMARY_UPDATE;


/**
 * Maintains a UI-centric model of the topology, as inferred from interactions
 * with the different (device, host, link, ...) services. Will serve up this
 * model to anyone who cares to {@link TopoUiListener listen}.
 */
@Component(immediate = true)
@Service
public class TopoUiModelManager implements TopoUiModelService {

    // TODO: put back to 30,000 ms for production
    private static final long SUMMARY_PERIOD = 15_000;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;


    private final ModelListenerRegistry listenerRegistry =
            new ModelListenerRegistry();

    private final TopoMessageFactory messageFactory = new TopoMessageFactory();
    private final MetaDb metaDb = new MetaDb();

    private final Timer timer = new Timer("topology-view");

    private TimerTask summaryTask = null;
    private boolean summaryRunning = false;


    @Activate
    public void activate() {
        eventDispatcher.addSink(TopoUiEvent.class, listenerRegistry);
        messageFactory.injectServices(
                metaDb,
                clusterService,
                deviceService,
                linkService,
                hostService,
                mastershipService
                // TODO: others??
        );
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(TopoUiEvent.class);
        log.info("Stopped");
    }


    @Override
    public void addListener(TopoUiListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(TopoUiListener listener) {
        listenerRegistry.removeListener(listener);
    }

    @Override
    public List<ObjectNode> getInitialState() {
        List<ObjectNode> results = new ArrayList<>();
        addInstances(results);
        addDevices(results);
        addLinks(results);
        addHosts(results);
        return results;
    }

    @Override
    public synchronized void startSummaryMonitoring() {
        // first, cancel previous task if not canceled already
        stopSummaryMonitoring();

        // create and start a summary task, to execute with no delay, and
        // every SUMMARY_PERIOD milliseconds thereafter.
        summaryTask = new TimerTask() {
            @Override
            public void run() {
                if (summaryRunning) {
                    post(new TopoUiEvent(SUMMARY_UPDATE, null));
                }
            }
        };

        timer.schedule(summaryTask, 0, SUMMARY_PERIOD);
        summaryRunning = true;
    }

    @Override
    public synchronized void stopSummaryMonitoring() {
        if (summaryTask != null) {
            summaryTask.cancel();
            summaryTask = null;
        }
        summaryRunning = false;
    }

    @Override
    public SummaryData getSummaryData() {
        return new SummaryDataImpl();
    }

    // =====================================================================

    private final class SummaryDataImpl implements SummaryData {
        private final Topology topology = topologyService.currentTopology();

        @Override
        public int deviceCount() {
            return topology.deviceCount();
        }

        @Override
        public int linkCount() {
            return topology.linkCount();
        }

        @Override
        public int hostCount() {
            return hostService.getHostCount();
        }

        @Override
        public int clusterCount() {
            return topology.clusterCount();
        }

        @Override
        public long intentCount() {
            return intentService.getIntentCount();
        }

        @Override
        public int flowRuleCount() {
            return flowRuleService.getFlowRuleCount();
        }
    }

    // =====================================================================

    private static final Comparator<? super ControllerNode> NODE_COMPARATOR =
            (o1, o2) -> o1.id().toString().compareTo(o2.id().toString());

    // =====================================================================

    private void addInstances(List<ObjectNode> results) {
        List<ControllerNode> nodes = new ArrayList<>(clusterService.getNodes());
        Collections.sort(nodes, NODE_COMPARATOR);
        for (ControllerNode node : nodes) {
            ClusterEvent ev = new ClusterEvent(INSTANCE_ADDED, node);
            results.add(messageFactory.instanceMessage(ev));
        }
    }

    private void addDevices(List<ObjectNode> results) {
        // Send optical first, others later -- for layered rendering
        List<DeviceEvent> deferred = new ArrayList<>();

        for (Device device : deviceService.getDevices()) {
            DeviceEvent ev = new DeviceEvent(DEVICE_ADDED, device);
            if (device.type() == Device.Type.ROADM) {
                results.add(messageFactory.deviceMessage(ev));
            } else {
                deferred.add(ev);
            }
        }

        for (DeviceEvent ev : deferred) {
            results.add(messageFactory.deviceMessage(ev));
        }
    }

    private void addLinks(List<ObjectNode> results) {
        // Send optical first, others later -- for layered rendering
        List<LinkEvent> deferred = new ArrayList<>();

        for (Link link : linkService.getLinks()) {
            LinkEvent ev = new LinkEvent(LINK_ADDED, link);
            if (link.type() == Link.Type.OPTICAL) {
                results.add(messageFactory.linkMessage(ev));
            } else {
                deferred.add(ev);
            }
        }

        for (LinkEvent ev : deferred) {
            results.add(messageFactory.linkMessage(ev));
        }
    }

    private void addHosts(List<ObjectNode> results) {
        for (Host host : hostService.getHosts()) {
            HostEvent ev = new HostEvent(HOST_ADDED, host);
            results.add(messageFactory.hostMessage(ev));
        }
    }

    // =====================================================================

    private void post(TopoUiEvent event) {
        if (event != null) {
            eventDispatcher.post(event);
        }
    }

    // NOTE: session-independent state only
    // private inner classes to listen to device/host/link events
    // TODO..
}
