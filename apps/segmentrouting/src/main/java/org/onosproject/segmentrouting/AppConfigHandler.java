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
package org.onosproject.segmentrouting;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.DefaultObjectiveContext;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.SegmentRoutingAppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles Segment Routing app config events.
 */
public class AppConfigHandler {
    private static final Logger log = LoggerFactory.getLogger(AppConfigHandler.class);
    private final SegmentRoutingManager srManager;
    private final DeviceService deviceService;

    /**
     * Constructs Segment Routing App Config Handler.
     *
     * @param srManager instance of {@link SegmentRoutingManager}
     */
    public AppConfigHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        this.deviceService = srManager.deviceService;
    }

    /**
     * Processes Segment Routing App Config added event.
     *
     * @param event network config added event
     */
    protected void processAppConfigAdded(NetworkConfigEvent event) {
        log.info("Processing AppConfig CONFIG_ADDED");
        SegmentRoutingAppConfig config = (SegmentRoutingAppConfig) event.config().get();
        deviceService.getAvailableDevices().forEach(device -> {
            populateVRouter(device.id(), getMacAddresses(config));
        });
    }

    /**
     * Processes Segment Routing App Config updated event.
     *
     * @param event network config updated event
     */
    protected void processAppConfigUpdated(NetworkConfigEvent event) {
        log.info("Processing AppConfig CONFIG_UPDATED");
        SegmentRoutingAppConfig config = (SegmentRoutingAppConfig) event.config().get();
        SegmentRoutingAppConfig prevConfig = (SegmentRoutingAppConfig) event.prevConfig().get();
        deviceService.getAvailableDevices().forEach(device -> {
            Set<MacAddress> macAddresses = new HashSet<>(getMacAddresses(config));
            Set<MacAddress> prevMacAddresses = new HashSet<>(getMacAddresses(prevConfig));
            // Avoid removing and re-adding unchanged MAC addresses since
            // FlowObjective does not guarantee the execution order.
            Set<MacAddress> sameMacAddresses = new HashSet<>(macAddresses);
            sameMacAddresses.retainAll(prevMacAddresses);
            macAddresses.removeAll(sameMacAddresses);
            prevMacAddresses.removeAll(sameMacAddresses);

            revokeVRouter(device.id(), prevMacAddresses);
            populateVRouter(device.id(), macAddresses);
        });

    }

    /**
     * Processes Segment Routing App Config removed event.
     *
     * @param event network config removed event
     */
    protected void processAppConfigRemoved(NetworkConfigEvent event) {
        log.info("Processing AppConfig CONFIG_REMOVED");
        SegmentRoutingAppConfig prevConfig = (SegmentRoutingAppConfig) event.prevConfig().get();
        deviceService.getAvailableDevices().forEach(device -> {
            revokeVRouter(device.id(), getMacAddresses(prevConfig));
        });
    }

    /**
     * Populates initial vRouter rules.
     *
     * @param deviceId device ID
     */
    public void init(DeviceId deviceId) {
        SegmentRoutingAppConfig config =
                srManager.cfgService.getConfig(srManager.appId, SegmentRoutingAppConfig.class);
        populateVRouter(deviceId, getMacAddresses(config));
    }

    private void populateVRouter(DeviceId deviceId, Set<MacAddress> pendingAdd) {
        if (!isEdge(deviceId)) {
            return;
        }
        getVRouterFlowObjBuilders(pendingAdd).forEach(foBuilder -> {
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("vRouterMac filter for {} populated", pendingAdd),
                    (objective, error) ->
                            log.warn("Failed to populate vRouterMac filter for {}: {}", pendingAdd, error));
            srManager.flowObjectiveService.filter(deviceId, foBuilder.add(context));
        });
    }

    private void revokeVRouter(DeviceId deviceId, Set<MacAddress> pendingRemove) {
        if (!isEdge(deviceId)) {
            return;
        }
        getVRouterFlowObjBuilders(pendingRemove).forEach(foBuilder -> {
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("vRouterMac filter for {} revoked", pendingRemove),
                    (objective, error) ->
                            log.warn("Failed to revoke vRouterMac filter for {}: {}", pendingRemove, error));
            srManager.flowObjectiveService.filter(deviceId, foBuilder.remove(context));
        });
    }

    private Set<FilteringObjective.Builder> getVRouterFlowObjBuilders(Set<MacAddress> macAddresses) {
        ImmutableSet.Builder<FilteringObjective.Builder> setBuilder = ImmutableSet.builder();
        macAddresses.forEach(macAddress -> {
            FilteringObjective.Builder fobuilder = DefaultFilteringObjective.builder();
            fobuilder.withKey(Criteria.matchInPort(PortNumber.ANY))
                    .addCondition(Criteria.matchEthDst(macAddress))
                    .permit()
                    .withPriority(SegmentRoutingService.DEFAULT_PRIORITY)
                    .fromApp(srManager.appId);
            setBuilder.add(fobuilder);
        });
        return setBuilder.build();
    }

    private Set<MacAddress> getMacAddresses(SegmentRoutingAppConfig config) {
        if (config == null) {
            return ImmutableSet.of();
        }
        return ImmutableSet.copyOf(config.vRouterMacs());
    }

    private boolean isEdge(DeviceId deviceId) {
        try {
            if (srManager.deviceConfiguration.isEdgeDevice(deviceId)) {
                return true;
            }
        } catch (DeviceConfigNotFoundException e) { }
        return false;
    }
}
