/*
 *  Copyright 2016-present Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.impl.topo.model;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.incubator.net.PortStatisticsService;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionEvent;
import org.onosproject.net.region.RegionListener;
import org.onosproject.net.region.RegionService;
import org.onosproject.net.statistic.StatisticService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.ui.impl.topo.UiTopoSession;
import org.onosproject.ui.model.ServiceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that creates and maintains the UI-model of the network topology.
 */
@Component(immediate = true, enabled = true)
@Service(value = UiSharedTopologyModel.class)
public final class UiSharedTopologyModel
        extends AbstractListenerManager<UiModelEvent, UiModelListener> {

    private static final Logger log =
            LoggerFactory.getLogger(UiSharedTopologyModel.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ClusterService clusterService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private MastershipService mastershipService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private RegionService regionService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private LinkService linkService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostService hostService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private IntentService intentService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowRuleService flowService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private StatisticService flowStatsService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private PortStatisticsService portStatsService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private TopologyService topologyService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private TunnelService tunnelService;

    private final ClusterEventListener clusterListener =
            new InternalClusterListener();
    private final MastershipListener mastershipListener =
            new InternalMastershipListener();
    private final RegionListener regionListener =
            new InternalRegionListener();
    private final DeviceListener deviceListener =
            new InternalDeviceListener();
    private final LinkListener linkListener =
            new InternalLinkListener();
    private final HostListener hostListener =
            new InternalHostListener();
    private final IntentListener intentListener =
            new InternalIntentListener();
    private final FlowRuleListener flowRuleListener =
            new InternalFlowRuleListener();

    private ExecutorService eventHandler;


    private ModelCache cache;


    @Activate
    protected void activate() {
        cache = new ModelCache(new DefaultServiceBundle(), eventDispatcher);
        eventHandler = Executors.newSingleThreadExecutor(Tools.groupedThreads("onos/ui/topo", "event-handler"));

        eventDispatcher.addSink(UiModelEvent.class, listenerRegistry);

        clusterService.addListener(clusterListener);
        mastershipService.addListener(mastershipListener);
        regionService.addListener(regionListener);
        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);
        hostService.addListener(hostListener);
        intentService.addListener(intentListener);
        flowService.addListener(flowRuleListener);

        cache.load();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        eventDispatcher.removeSink(UiModelEvent.class);

        clusterService.removeListener(clusterListener);
        mastershipService.removeListener(mastershipListener);
        regionService.removeListener(regionListener);
        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);
        hostService.removeListener(hostListener);
        intentService.removeListener(intentListener);
        flowService.removeListener(flowRuleListener);

        eventHandler.shutdown();

        cache.clear();
        cache = null;

        log.info("Stopped");
    }


    /**
     * Registers a UI topology session with the topology model.
     *
     * @param session the session to register
     */
    public void register(UiTopoSession session) {
        log.info("Registering topology session {}", session);
        addListener(session);
    }

    /**
     * Unregisters a UI topology session from the topology model.
     *
     * @param session the session to unregister
     */
    public void unregister(UiTopoSession session) {
        log.info("Unregistering topology session {}", session);
        removeListener(session);
    }

    /**
     * Default implementation of service bundle to return references to our
     * dynamically injected services.
     */
    private class DefaultServiceBundle implements ServiceBundle {
        @Override
        public ClusterService cluster() {
            return clusterService;
        }

        @Override
        public MastershipService mastership() {
            return mastershipService;
        }

        @Override
        public RegionService region() {
            return regionService;
        }

        @Override
        public DeviceService device() {
            return deviceService;
        }

        @Override
        public LinkService link() {
            return linkService;
        }

        @Override
        public HostService host() {
            return hostService;
        }

        @Override
        public IntentService intent() {
            return intentService;
        }

        @Override
        public FlowRuleService flow() {
            return flowService;
        }
    }


    private class InternalClusterListener implements ClusterEventListener {
        @Override
        public void event(ClusterEvent event) {
            eventHandler.execute(() -> handleEvent(event));
        }

        private void handleEvent(ClusterEvent event) {
            ControllerNode cnode = event.subject();

            switch (event.type()) {

                case INSTANCE_ADDED:
                case INSTANCE_ACTIVATED:
                case INSTANCE_READY:
                case INSTANCE_DEACTIVATED:
                    cache.addOrUpdateClusterMember(cnode);
                    break;

                case INSTANCE_REMOVED:
                    cache.removeClusterMember(cnode);
                    break;

                default:
                    break;
            }
        }
    }

    private class InternalMastershipListener implements MastershipListener {
        @Override
        public void event(MastershipEvent event) {
            DeviceId deviceId = event.subject();
            RoleInfo roleInfo = event.roleInfo();

            switch (event.type()) {
                case MASTER_CHANGED:
                case BACKUPS_CHANGED:
                    cache.updateMasterships(deviceId, roleInfo);
                    break;

                default:
                    break;
            }
        }
    }

    private class InternalRegionListener implements RegionListener {
        @Override
        public void event(RegionEvent event) {
            Region region = event.subject();

            switch (event.type()) {

                case REGION_ADDED:
                case REGION_UPDATED:
                case REGION_MEMBERSHIP_CHANGED:
                    cache.addOrUpdateRegion(region);
                    break;

                case REGION_REMOVED:
                    cache.removeRegion(region);
                    break;

                default:
                    break;
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();

            switch (event.type()) {

                case DEVICE_ADDED:
                case DEVICE_UPDATED:
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_SUSPENDED:
                    cache.addOrUpdateDevice(device);
                    break;

                case DEVICE_REMOVED:
                    cache.removeDevice(device);
                    break;

                default:
                    break;
            }
        }
    }

    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            Link link = event.subject();

            switch (event.type()) {

                case LINK_ADDED:
                case LINK_UPDATED:
                    cache.addOrUpdateLink(link);
                    break;

                case LINK_REMOVED:
                    cache.removeLink(link);
                    break;

                default:
                    break;
            }
        }
    }

    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            Host host = event.subject();
            Host prevHost = event.prevSubject();

            switch (event.type()) {

                case HOST_ADDED:
                case HOST_UPDATED:
                    cache.addOrUpdateHost(host);
                    break;

                case HOST_MOVED:
                    cache.moveHost(host, prevHost);
                    break;

                case HOST_REMOVED:
                    cache.removeHost(host);
                    break;

                default:
                    break;
            }
        }
    }

    // =======================================================================
    // NOTE: Neither intents nor flows are modeled by the UiTopology.
    //       Rather, they are serviced directly from this class.
    //       Additionally, since we are only retrieving counts (in the current
    //        implementation), we'll fetch them on demand from the service.
    //       Thus, the following internal listeners are stubs only (for now).
    // =======================================================================

    private class InternalIntentListener implements IntentListener {
        @Override
        public void event(IntentEvent event) {
            // do nothing (for now)
        }
    }

    private class InternalFlowRuleListener implements FlowRuleListener {
        @Override
        public void event(FlowRuleEvent event) {
            // do nothing (for now)
        }
    }

}
