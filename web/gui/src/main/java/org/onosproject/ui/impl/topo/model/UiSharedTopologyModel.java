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

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.incubator.net.PortStatisticsService;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
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
import org.onosproject.net.region.RegionEvent;
import org.onosproject.net.region.RegionListener;
import org.onosproject.net.region.RegionService;
import org.onosproject.net.statistic.StatisticService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.ui.impl.topo.UiTopoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * A lazily-initialized Singleton that creates and maintains the UI-model
 * of the network topology.
 */
public final class UiSharedTopologyModel {

    private static final Logger log =
            LoggerFactory.getLogger(UiSharedTopologyModel.class);

    private final ModelEventListener modelEventListener;

    private final Set<UiTopoSession> sessions = new HashSet<>();

    private UiSharedTopologyModel() {
        modelEventListener = new ModelEventListener().init();

        // TODO: build and maintain the state of the model
        // (1) query model for current state
        // (2) update state as model events arrive
    }

    /**
     * Registers a UI topology session with the topology model.
     *
     * @param session the session to register
     */
    public void register(UiTopoSession session) {
        log.info("Registering topology session {}", session);
        sessions.add(session);
    }

    /**
     * Unregisters a UI topology session from the topology model.
     *
     * @param session the session to unregister
     */
    public void unregister(UiTopoSession session) {
        log.info("Unregistering topology session {}", session);
        sessions.remove(session);
    }


    // TODO: notify registered sessions when changes happen to the model


    // ----------

    // inner class to encapsulate the model listeners
    private final class ModelEventListener {

        // TODO: Review - is this good enough? couldn't otherwise see how to inject
        private final ServiceDirectory directory = new DefaultServiceDirectory();

        private ClusterService clusterService;
        private MastershipService mastershipService;
        private RegionService regionService;
        private DeviceService deviceService;
        private LinkService linkService;
        private HostService hostService;
        private IntentService intentService;
        private FlowRuleService flowService;

        private StatisticService flowStatsService;
        private PortStatisticsService portStatsService;
        private TopologyService topologyService;
        private TunnelService tunnelService;

        private ModelEventListener init() {
            clusterService = directory.get(ClusterService.class);
            mastershipService = directory.get(MastershipService.class);
            regionService = directory.get(RegionService.class);
            deviceService = directory.get(DeviceService.class);
            linkService = directory.get(LinkService.class);
            hostService = directory.get(HostService.class);
            intentService = directory.get(IntentService.class);
            flowService = directory.get(FlowRuleService.class);

            // passive services (?) to whom we are not listening...
            flowStatsService = directory.get(StatisticService.class);
            portStatsService = directory.get(PortStatisticsService.class);
            topologyService = directory.get(TopologyService.class);
            tunnelService = directory.get(TunnelService.class);

            return this;
        }

        private class InternalClusterListener implements ClusterEventListener {
            @Override
            public void event(ClusterEvent event) {
                // TODO: handle cluster event
                // (1) emit cluster member event
            }
        }

        private class InternalMastershipListener implements MastershipListener {
            @Override
            public void event(MastershipEvent event) {
                // TODO: handle mastership event
                // (1) emit cluster member update for all members
                // (2) emit update device event for he whose mastership changed
            }
        }

        private class InternalRegionListener implements RegionListener {
            @Override
            public void event(RegionEvent event) {
                // TODO: handle region event
                // (1) emit region event
            }
        }

        private class InternalDeviceListener implements DeviceListener {
            @Override
            public void event(DeviceEvent event) {
                // TODO: handle device event
                // (1) emit device event
            }
        }

        private class InternalLinkListener implements LinkListener {
            @Override
            public void event(LinkEvent event) {
                // TODO: handle link event
                // (1) consolidate infrastructure links -> UiLink (?)
                // (2) emit link event
            }
        }

        private class InternalHostListener implements HostListener {
            @Override
            public void event(HostEvent event) {
                // TODO: handle host event
                // (1) emit host event
            }
        }

        private class InternalIntentListener implements IntentListener {
            @Override
            public void event(IntentEvent event) {
                // TODO: handle intent event
                // (1) update cache of intent counts?
            }
        }

        private class InternalFlowRuleListener implements FlowRuleListener {
            @Override
            public void event(FlowRuleEvent event) {
                // TODO: handle flowrule event
                // (1) update cache of flow counts?
            }
        }
    }

    // ----------

    /**
     * Bill Pugh Singleton pattern. INSTANCE won't be instantiated until the
     * LazyHolder class is loaded via a call to the instance() method below.
     */
    private static class LazyHolder {
        private static final UiSharedTopologyModel INSTANCE =
                new UiSharedTopologyModel();
    }

    /**
     * Returns a reference to the Singleton UI network topology model.
     *
     * @return the singleton topology model
     */
    public static UiSharedTopologyModel instance() {
        return LazyHolder.INSTANCE;
    }
}
