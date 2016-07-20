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
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.provider.ProviderId;
import org.opencord.cordconfig.CordConfigEvent;
import org.opencord.cordconfig.access.AccessAgentData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Handles access agent config event which is required for CORD integration.
 */
public class CordConfigHandler {
    private static Logger log = LoggerFactory.getLogger(CordConfigHandler.class);
    private final SegmentRoutingManager srManager;

    /**
     * Constructs the CordConfigHandler.
     *
     * @param srManager Segment Routing manager
     */
    public CordConfigHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
    }

    /**
     * Read initial access agent config for given device.
     *
     * @param deviceId ID of the device to be initialized
     */
    public void init(DeviceId deviceId) {
        // Try to read access agent config
        Optional<AccessAgentData> accessAgent =
                srManager.cordConfigService.getAccessAgent(deviceId);

        if (!accessAgent.isPresent()) {
            log.debug("No access agent config on {}. Skip.", deviceId);
            return;
        }

        processAccessAgentAdded(accessAgent.get());
    }

    // TODO javadoc
    protected void processAccessAgentAddedEvent(CordConfigEvent event) {
        log.debug("processAccessAgentAdded: {}, {}", event.subject(), event.prevSubject());
        processAccessAgentAdded((AccessAgentData) event.subject());
    }

    protected void processAccessAgentUpdatedEvent(CordConfigEvent event) {
        log.debug("processAccessAgentUpdated: {}, {}", event.subject(), event.prevSubject());
        processAccessAgentRemoved((AccessAgentData) event.prevSubject());
        processAccessAgentAdded((AccessAgentData) event.subject());
    }

    protected void processAccessAgentRemovedEvent(CordConfigEvent event) {
        log.debug("processAccessAgentRemoved: {}, {}", event.subject(), event.prevSubject());
        processAccessAgentRemoved((AccessAgentData) event.prevSubject());
    }

    protected void processAccessAgentAdded(AccessAgentData accessAgentData) {
        if (!srManager.mastershipService.isLocalMaster(accessAgentData.deviceId())) {
            log.debug("Not the master of {}. Abort.", accessAgentData.deviceId());
            return;
        }

        // Do not proceed if vtn location is missing
        if (!accessAgentData.getVtnLocation().isPresent()) {
            log.warn("accessAgentData does not contain vtn location. Abort.");
            return;
        }

        MacAddress agentMac = accessAgentData.getAgentMac();
        ConnectPoint agentLocation = accessAgentData.getVtnLocation().get();

        // Do not proceed if agent port doesn't have subnet configured
        Ip4Prefix agentSubnet = srManager.deviceConfiguration
                .getPortSubnet(agentLocation.deviceId(), agentLocation.port());
        if (agentSubnet == null) {
            log.warn("Agent port does not have subnet configuration. Abort.");
            return;
        }

        // Add host information for agent
        log.info("push host info for agent {}", agentMac);
        srManager.hostHandler.processHostAdded(createHost(agentMac, agentLocation));

        accessAgentData.getOltMacInfo().forEach((connectPoint, macAddress) -> {
            // Do not proceed if olt port has subnet configured
            Ip4Prefix oltSubnet = srManager.deviceConfiguration
                    .getPortSubnet(connectPoint.deviceId(), connectPoint.port());
            if (oltSubnet != null) {
                log.warn("OLT port has subnet configuration. Abort.");
                return;
            }

            // Add olt to the subnet of agent
            log.info("push subnet for olt {}", agentSubnet);
            srManager.deviceConfiguration.addSubnet(connectPoint, agentSubnet);
            srManager.routingRulePopulator.populateRouterMacVlanFilters(connectPoint.deviceId());

            // Add host information for olt
            log.info("push host info for olt {}", macAddress);
            srManager.hostHandler.processHostAdded(createHost(macAddress, connectPoint));
        });
    }

    protected void processAccessAgentRemoved(AccessAgentData accessAgentData) {
        if (!srManager.mastershipService.isLocalMaster(accessAgentData.deviceId())) {
            log.debug("Not the master of {}. Abort.", accessAgentData.deviceId());
            return;
        }

        // Do not proceed if vtn location is missing
        if (!accessAgentData.getVtnLocation().isPresent()) {
            log.warn("accessAgentData does not contain vtn location. Abort.");
            return;
        }

        MacAddress agentMac = accessAgentData.getAgentMac();
        ConnectPoint agentLocation = accessAgentData.getVtnLocation().get();

        // Do not proceed if olt port doesn't have subnet configured
        Ip4Prefix agentSubnet = srManager.deviceConfiguration
                .getPortSubnet(agentLocation.deviceId(), agentLocation.port());
        if (agentSubnet == null) {
            log.warn("Agent port does not have subnet configuration. Abort.");
            return;
        }

        // Remove host information for agent
        log.info("delete host info for agent {}", agentMac);
        srManager.hostHandler.processHostRemoved(createHost(agentMac, agentLocation));

        accessAgentData.getOltMacInfo().forEach((connectPoint, macAddress) -> {
            // Do not proceed if agent port doesn't have subnet configured
            Ip4Prefix oltSubnet = srManager.deviceConfiguration
                    .getPortSubnet(connectPoint.deviceId(), connectPoint.port());
            if (oltSubnet == null) {
                log.warn("OLT port does not have subnet configuration. Abort.");
                return;
            }

            // Remove host information for olt
            log.info("delete host info for olt {}", macAddress);
            srManager.hostHandler.processHostRemoved(createHost(macAddress, connectPoint));

            // Remove olt to the subnet of agent
            log.info("delete subnet for olt {}", agentSubnet);
            srManager.deviceConfiguration.removeSubnet(connectPoint, agentSubnet);
            srManager.routingRulePopulator.populateRouterMacVlanFilters(connectPoint.deviceId());
        });
    }

    private Host createHost(MacAddress macAddress, ConnectPoint location) {
        return new DefaultHost(
                new ProviderId("host", "org.onosproject.segmentrouting"),
                HostId.hostId(macAddress),
                macAddress,
                VlanId.NONE,
                new HostLocation(location, System.currentTimeMillis()),
                ImmutableSet.of());
    }
}
