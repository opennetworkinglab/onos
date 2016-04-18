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

import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultObjectiveContext;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Handles host-related events.
 */
public class HostHandler {
    private static final Logger log = LoggerFactory.getLogger(HostHandler.class);
    private final SegmentRoutingManager srManager;
    private CoreService coreService;
    private HostService hostService;
    private FlowObjectiveService flowObjectiveService;

    /**
     * Constructs the HostHandler.
     *
     * @param srManager Segment Routing manager
     */
    public HostHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        coreService = srManager.coreService;
        hostService = srManager.hostService;
        flowObjectiveService = srManager.flowObjectiveService;
    }

    protected void readInitialHosts() {
        hostService.getHosts().forEach(host -> {
            MacAddress mac = host.mac();
            VlanId vlanId = host.vlan();
            DeviceId deviceId = host.location().deviceId();
            PortNumber port = host.location().port();
            Set<IpAddress> ips = host.ipAddresses();
            log.debug("Host {}/{} is added at {}:{}", mac, vlanId, deviceId, port);

            // Populate bridging table entry
            ForwardingObjective.Builder fob =
                    getForwardingObjectiveBuilder(deviceId, mac, vlanId, port);
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Host rule for {} populated", host),
                    (objective, error) ->
                            log.warn("Failed to populate host rule for {}: {}", host, error));
            flowObjectiveService.forward(deviceId, fob.add(context));

            // Populate IP table entry
            ips.forEach(ip -> {
                if (ip.isIp4()) {
                    srManager.routingRulePopulator.populateIpRuleForHost(
                            deviceId, ip.getIp4Address(), mac, port);
                }
            });
        });
    }

    private ForwardingObjective.Builder getForwardingObjectiveBuilder(
            DeviceId deviceId, MacAddress mac, VlanId vlanId,
            PortNumber outport) {
        // Get assigned VLAN for the subnet
        VlanId outvlan = null;
        Ip4Prefix subnet = srManager.deviceConfiguration.getPortSubnet(deviceId, outport);
        if (subnet == null) {
            outvlan = VlanId.vlanId(SegmentRoutingManager.ASSIGNED_VLAN_NO_SUBNET);
        } else {
            outvlan = srManager.getSubnetAssignedVlanId(deviceId, subnet);
        }

        // match rule
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        sbuilder.matchEthDst(mac);
            /*
             * Note: for untagged packets, match on the assigned VLAN.
             *       for tagged packets, match on its incoming VLAN.
             */
        if (vlanId.equals(VlanId.NONE)) {
            sbuilder.matchVlanId(outvlan);
        } else {
            sbuilder.matchVlanId(vlanId);
        }

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        tbuilder.immediate().popVlan();
        tbuilder.immediate().setOutput(outport);

        // for switch pipelines that need it, provide outgoing vlan as metadata
        TrafficSelector meta = DefaultTrafficSelector.builder()
                .matchVlanId(outvlan).build();

        // All forwarding is via Groups. Drivers can re-purpose to flow-actions if needed.
        int portNextObjId = srManager.getPortNextObjectiveId(deviceId, outport,
                tbuilder.build(),
                meta);

        return DefaultForwardingObjective.builder()
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .withSelector(sbuilder.build())
                .nextStep(portNextObjId)
                .withPriority(100)
                .fromApp(srManager.appId)
                .makePermanent();
    }

    protected void processHostAddedEvent(HostEvent event) {
        MacAddress mac = event.subject().mac();
        VlanId vlanId = event.subject().vlan();
        DeviceId deviceId = event.subject().location().deviceId();
        PortNumber port = event.subject().location().port();
        Set<IpAddress> ips = event.subject().ipAddresses();
        log.info("Host {}/{} is added at {}:{}", mac, vlanId, deviceId, port);

        if (!srManager.deviceConfiguration.suppressHost()
                .contains(new ConnectPoint(deviceId, port))) {
            // Populate bridging table entry
            log.debug("Populate L2 table entry for host {} at {}:{}",
                    mac, deviceId, port);
            ForwardingObjective.Builder fob =
                    getForwardingObjectiveBuilder(deviceId, mac, vlanId, port);
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Host rule for {} populated", event.subject()),
                    (objective, error) ->
                            log.warn("Failed to populate host rule for {}: {}", event.subject(), error));
            flowObjectiveService.forward(deviceId, fob.add(context));

            // Populate IP table entry
            ips.forEach(ip -> {
                if (ip.isIp4()) {
                    srManager.routingRulePopulator.populateIpRuleForHost(
                            deviceId, ip.getIp4Address(), mac, port);
                }
            });
        }
    }

    protected void processHostRemoveEvent(HostEvent event) {
        MacAddress mac = event.subject().mac();
        VlanId vlanId = event.subject().vlan();
        DeviceId deviceId = event.subject().location().deviceId();
        PortNumber port = event.subject().location().port();
        Set<IpAddress> ips = event.subject().ipAddresses();
        log.debug("Host {}/{} is removed from {}:{}", mac, vlanId, deviceId, port);

        if (!srManager.deviceConfiguration.suppressHost()
                .contains(new ConnectPoint(deviceId, port))) {
            // Revoke bridging table entry
            ForwardingObjective.Builder fob =
                    getForwardingObjectiveBuilder(deviceId, mac, vlanId, port);
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Host rule for {} revoked", event.subject()),
                    (objective, error) ->
                            log.warn("Failed to revoke host rule for {}: {}", event.subject(), error));
            flowObjectiveService.forward(deviceId, fob.remove(context));

            // Revoke IP table entry
            ips.forEach(ip -> {
                if (ip.isIp4()) {
                    srManager.routingRulePopulator.revokeIpRuleForHost(
                            deviceId, ip.getIp4Address(), mac, port);
                }
            });
        }
    }

    protected void processHostMovedEvent(HostEvent event) {
        MacAddress mac = event.subject().mac();
        VlanId vlanId = event.subject().vlan();
        DeviceId prevDeviceId = event.prevSubject().location().deviceId();
        PortNumber prevPort = event.prevSubject().location().port();
        Set<IpAddress> prevIps = event.prevSubject().ipAddresses();
        DeviceId newDeviceId = event.subject().location().deviceId();
        PortNumber newPort = event.subject().location().port();
        Set<IpAddress> newIps = event.subject().ipAddresses();
        log.debug("Host {}/{} is moved from {}:{} to {}:{}",
                mac, vlanId, prevDeviceId, prevPort, newDeviceId, newPort);

        if (!srManager.deviceConfiguration.suppressHost()
                .contains(new ConnectPoint(prevDeviceId, prevPort))) {
            // Revoke previous bridging table entry
            ForwardingObjective.Builder prevFob =
                    getForwardingObjectiveBuilder(prevDeviceId, mac, vlanId, prevPort);
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Host rule for {} revoked", event.subject()),
                    (objective, error) ->
                            log.warn("Failed to revoke host rule for {}: {}", event.subject(), error));
            flowObjectiveService.forward(prevDeviceId, prevFob.remove(context));

            // Revoke previous IP table entry
            prevIps.forEach(ip -> {
                if (ip.isIp4()) {
                    srManager.routingRulePopulator.revokeIpRuleForHost(
                            prevDeviceId, ip.getIp4Address(), mac, prevPort);
                }
            });
        }

        if (!srManager.deviceConfiguration.suppressHost()
                .contains(new ConnectPoint(newDeviceId, newPort))) {
            // Populate new bridging table entry
            ForwardingObjective.Builder newFob =
                    getForwardingObjectiveBuilder(newDeviceId, mac, vlanId, newPort);
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Host rule for {} populated", event.subject()),
                    (objective, error) ->
                            log.warn("Failed to populate host rule for {}: {}", event.subject(), error));
            flowObjectiveService.forward(newDeviceId, newFob.add(context));

            // Populate new IP table entry
            newIps.forEach(ip -> {
                if (ip.isIp4()) {
                    srManager.routingRulePopulator.populateIpRuleForHost(
                            newDeviceId, ip.getIp4Address(), mac, newPort);
                }
            });
        }
    }

    protected void processHostUpdatedEvent(HostEvent event) {
        MacAddress mac = event.subject().mac();
        VlanId vlanId = event.subject().vlan();
        DeviceId prevDeviceId = event.prevSubject().location().deviceId();
        PortNumber prevPort = event.prevSubject().location().port();
        Set<IpAddress> prevIps = event.prevSubject().ipAddresses();
        DeviceId newDeviceId = event.subject().location().deviceId();
        PortNumber newPort = event.subject().location().port();
        Set<IpAddress> newIps = event.subject().ipAddresses();
        log.debug("Host {}/{} is updated", mac, vlanId);

        if (!srManager.deviceConfiguration.suppressHost()
                .contains(new ConnectPoint(prevDeviceId, prevPort))) {
            // Revoke previous IP table entry
            prevIps.forEach(ip -> {
                if (ip.isIp4()) {
                    srManager.routingRulePopulator.revokeIpRuleForHost(
                            prevDeviceId, ip.getIp4Address(), mac, prevPort);
                }
            });
        }

        if (!srManager.deviceConfiguration.suppressHost()
                .contains(new ConnectPoint(newDeviceId, newPort))) {
            // Populate new IP table entry
            newIps.forEach(ip -> {
                if (ip.isIp4()) {
                    srManager.routingRulePopulator.populateIpRuleForHost(
                            newDeviceId, ip.getIp4Address(), mac, newPort);
                }
            });
        }
    }
}
