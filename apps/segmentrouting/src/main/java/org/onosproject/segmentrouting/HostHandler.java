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
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostLocation;
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
import org.onosproject.segmentrouting.config.SegmentRoutingAppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Handles host-related events.
 */
public class HostHandler {
    private static final Logger log = LoggerFactory.getLogger(HostHandler.class);
    private final SegmentRoutingManager srManager;
    private HostService hostService;
    private FlowObjectiveService flowObjectiveService;

    /**
     * Constructs the HostHandler.
     *
     * @param srManager Segment Routing manager
     */
    public HostHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        hostService = srManager.hostService;
        flowObjectiveService = srManager.flowObjectiveService;
    }

    protected void readInitialHosts(DeviceId devId) {
        hostService.getHosts().forEach(host -> {
            DeviceId deviceId = host.location().deviceId();
            // The host does not attach to this device
            if (!deviceId.equals(devId)) {
                return;
            }
            processHostAdded(host);
        });
    }

    protected void processHostAddedEvent(HostEvent event) {
        processHostAdded(event.subject());
    }

    protected void processHostAdded(Host host) {
        MacAddress mac = host.mac();
        VlanId vlanId = host.vlan();
        HostLocation location = host.location();
        DeviceId deviceId = location.deviceId();
        PortNumber port = location.port();
        Set<IpAddress> ips = host.ipAddresses();
        log.debug("Host {}/{} is added at {}:{}", mac, vlanId, deviceId, port);

        if (accepted(host)) {
            // Populate bridging table entry
            log.debug("Populate L2 table entry for host {} at {}:{}",
                    mac, deviceId, port);
            ForwardingObjective.Builder fob =
                    hostFwdObjBuilder(deviceId, mac, vlanId, port);
            if (fob == null) {
                log.warn("Fail to create fwd obj for host {}/{}. Abort.", mac, vlanId);
                return;
            }
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Host rule for {}/{} populated", mac, vlanId),
                    (objective, error) ->
                            log.warn("Failed to populate host rule for {}/{}: {}", mac, vlanId, error));
            flowObjectiveService.forward(deviceId, fob.add(context));

            ips.forEach(ip -> {
                // Populate IP table entry
                if (ip.isIp4()) {
                    addPerHostRoute(location, ip.getIp4Address());
                    srManager.routingRulePopulator.populateRoute(
                            deviceId, ip.toIpPrefix(), mac, port);
                }
            });
        }
    }

    protected void processHostRemoveEvent(HostEvent event) {
        processHostRemoved(event.subject());
    }

    protected void processHostRemoved(Host host) {
        MacAddress mac = host.mac();
        VlanId vlanId = host.vlan();
        HostLocation location = host.location();
        DeviceId deviceId = location.deviceId();
        PortNumber port = location.port();
        Set<IpAddress> ips = host.ipAddresses();
        log.debug("Host {}/{} is removed from {}:{}", mac, vlanId, deviceId, port);

        if (accepted(host)) {
            // Revoke bridging table entry
            ForwardingObjective.Builder fob =
                    hostFwdObjBuilder(deviceId, mac, vlanId, port);
            if (fob == null) {
                log.warn("Fail to create fwd obj for host {}/{}. Abort.", mac, vlanId);
                return;
            }
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Host rule for {} revoked", host),
                    (objective, error) ->
                            log.warn("Failed to revoke host rule for {}: {}", host, error));
            flowObjectiveService.forward(deviceId, fob.remove(context));

            // Revoke IP table entry
            ips.forEach(ip -> {
                if (ip.isIp4()) {
                    removePerHostRoute(location, ip.getIp4Address());
                    srManager.routingRulePopulator.revokeRoute(
                            deviceId, ip.toIpPrefix(), mac, port);
                }
            });
        }
    }

    protected void processHostMovedEvent(HostEvent event) {
        MacAddress mac = event.subject().mac();
        VlanId vlanId = event.subject().vlan();
        HostLocation prevLocation = event.prevSubject().location();
        DeviceId prevDeviceId = prevLocation.deviceId();
        PortNumber prevPort = prevLocation.port();
        Set<IpAddress> prevIps = event.prevSubject().ipAddresses();
        HostLocation newLocation = event.subject().location();
        DeviceId newDeviceId = newLocation.deviceId();
        PortNumber newPort = newLocation.port();
        Set<IpAddress> newIps = event.subject().ipAddresses();
        log.debug("Host {}/{} is moved from {}:{} to {}:{}",
                mac, vlanId, prevDeviceId, prevPort, newDeviceId, newPort);

        if (accepted(event.prevSubject())) {
            // Revoke previous bridging table entry
            ForwardingObjective.Builder prevFob =
                    hostFwdObjBuilder(prevDeviceId, mac, vlanId, prevPort);
            if (prevFob == null) {
                log.warn("Fail to create fwd obj for host {}/{}. Abort.", mac, vlanId);
                return;
            }
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Host rule for {} revoked", event.subject()),
                    (objective, error) ->
                            log.warn("Failed to revoke host rule for {}: {}", event.subject(), error));
            flowObjectiveService.forward(prevDeviceId, prevFob.remove(context));

            // Revoke previous IP table entry
            prevIps.forEach(ip -> {
                if (ip.isIp4()) {
                    removePerHostRoute(prevLocation, ip.getIp4Address());
                    srManager.routingRulePopulator.revokeRoute(
                            prevDeviceId, ip.toIpPrefix(), mac, prevPort);
                }
            });
        }

        if (accepted(event.subject())) {
            // Populate new bridging table entry
            ForwardingObjective.Builder newFob =
                    hostFwdObjBuilder(newDeviceId, mac, vlanId, newPort);
            if (newFob == null) {
                log.warn("Fail to create fwd obj for host {}/{}. Abort.", mac, vlanId);
                return;
            }
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Host rule for {} populated", event.subject()),
                    (objective, error) ->
                            log.warn("Failed to populate host rule for {}: {}", event.subject(), error));
            flowObjectiveService.forward(newDeviceId, newFob.add(context));

            // Populate new IP table entry
            newIps.forEach(ip -> {
                if (ip.isIp4()) {
                    addPerHostRoute(newLocation, ip.getIp4Address());
                    srManager.routingRulePopulator.populateRoute(
                            newDeviceId, ip.toIpPrefix(), mac, newPort);
                }
            });
        }
    }

    protected void processHostUpdatedEvent(HostEvent event) {
        MacAddress mac = event.subject().mac();
        VlanId vlanId = event.subject().vlan();
        HostLocation prevLocation = event.prevSubject().location();
        DeviceId prevDeviceId = prevLocation.deviceId();
        PortNumber prevPort = prevLocation.port();
        Set<IpAddress> prevIps = event.prevSubject().ipAddresses();
        HostLocation newLocation = event.subject().location();
        DeviceId newDeviceId = newLocation.deviceId();
        PortNumber newPort = newLocation.port();
        Set<IpAddress> newIps = event.subject().ipAddresses();
        log.debug("Host {}/{} is updated", mac, vlanId);

        if (accepted(event.prevSubject())) {
            // Revoke previous IP table entry
            prevIps.forEach(ip -> {
                if (ip.isIp4()) {
                    removePerHostRoute(prevLocation, ip.getIp4Address());
                    srManager.routingRulePopulator.revokeRoute(
                            prevDeviceId, ip.toIpPrefix(), mac, prevPort);
                }
            });
        }

        if (accepted(event.subject())) {
            // Populate new IP table entry
            newIps.forEach(ip -> {
                if (ip.isIp4()) {
                    addPerHostRoute(newLocation, ip.getIp4Address());
                    srManager.routingRulePopulator.populateRoute(
                            newDeviceId, ip.toIpPrefix(), mac, newPort);
                }
            });
        }
    }

    /**
     * Generates the forwarding objective builder for the host rules.
     *
     * @param deviceId Device that host attaches to
     * @param mac MAC address of the host
     * @param vlanId VLAN ID of the host
     * @param outport Port that host attaches to
     * @return Forwarding objective builder
     */
    private ForwardingObjective.Builder hostFwdObjBuilder(
            DeviceId deviceId, MacAddress mac, VlanId vlanId,
            PortNumber outport) {
        // Get assigned VLAN for the subnets
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
        if (portNextObjId == -1) {
            // warning log will come from getPortNextObjective method
            return null;
        }

        return DefaultForwardingObjective.builder()
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .withSelector(sbuilder.build())
                .nextStep(portNextObjId)
                .withPriority(100)
                .fromApp(srManager.appId)
                .makePermanent();
    }

    /**
     * Add per host route to subnet list and populate the flow rule if the host
     * does not belong to the configured subnet.
     *
     * @param location location of the host being added
     * @param ip IP address of the host being added
     */
    private void addPerHostRoute(ConnectPoint location, Ip4Address ip) {
        Ip4Prefix portSubnet = srManager.deviceConfiguration.getPortSubnet(
                location.deviceId(), location.port());
        if (portSubnet != null && !portSubnet.contains(ip)) {
            Ip4Prefix ip4Prefix = ip.toIpPrefix().getIp4Prefix();
            srManager.deviceConfiguration.addSubnet(location, ip4Prefix);
            srManager.defaultRoutingHandler.populateSubnet(location,
                    ImmutableSet.of(ip4Prefix));
        }
    }

    /**
     * Remove per host route from subnet list and revoke the flow rule if the
     * host does not belong to the configured subnet.
     *
     * @param location location of the host being removed
     * @param ip IP address of the host being removed
     */
    private void removePerHostRoute(ConnectPoint location, Ip4Address ip) {
        Ip4Prefix portSubnet = srManager.deviceConfiguration.getPortSubnet(
                location.deviceId(), location.port());
        if (portSubnet != null && !portSubnet.contains(ip)) {
            Ip4Prefix ip4Prefix = ip.toIpPrefix().getIp4Prefix();
            srManager.deviceConfiguration.removeSubnet(location, ip4Prefix);
            srManager.defaultRoutingHandler.revokeSubnet(ImmutableSet.of(ip4Prefix));
        }
    }

    /**
     * Check if a host is accepted or not.
     *
     * @param host host to be checked
     * @return true if segment routing accepts the host
     */
    private boolean accepted(Host host) {
        SegmentRoutingAppConfig appConfig = srManager.cfgService
                .getConfig(srManager.appId, SegmentRoutingAppConfig.class);

        boolean accepted = appConfig == null ||
                (!appConfig.suppressHostByProvider().contains(host.providerId().id()) &&
                !appConfig.suppressHostByPort().contains(host.location()));
        if (!accepted) {
            log.info("Ignore suppressed host {}", host.id());
        }
        return accepted;
    }
}
