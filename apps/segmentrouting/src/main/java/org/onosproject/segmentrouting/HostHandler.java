/*
 * Copyright 2016-present Open Networking Foundation
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Handles host-related events.
 */
public class HostHandler {

    private static final Logger log = LoggerFactory.getLogger(HostHandler.class);
    protected final SegmentRoutingManager srManager;
    private HostService hostService;
    private FlowObjectiveService flowObjectiveService;

    /**
     * Constructs the HostHandler.
     *
     * @param srManager Segment Routing manager
     */
    HostHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        hostService = srManager.hostService;
        flowObjectiveService = srManager.flowObjectiveService;
    }

    protected void init(DeviceId devId) {
        hostService.getHosts().forEach(host ->
            host.locations().stream()
                    .filter(location -> location.deviceId().equals(devId))
                    .forEach(location -> processHostAddedAtLocation(host, location))
        );
    }

    void processHostAddedEvent(HostEvent event) {
        processHostAdded(event.subject());
    }

    private void processHostAdded(Host host) {
        host.locations().forEach(location -> processHostAddedAtLocation(host, location));
    }

    void processHostAddedAtLocation(Host host, HostLocation location) {
        checkArgument(host.locations().contains(location), "{} is not a location of {}", location, host);

        MacAddress hostMac = host.mac();
        VlanId hostVlanId = host.vlan();
        Set<HostLocation> locations = host.locations();
        Set<IpAddress> ips = host.ipAddresses();
        log.info("Host {}/{} is added at {}", hostMac, hostVlanId, locations);

        if (srManager.isMasterOf(location)) {
            processBridgingRule(location.deviceId(), location.port(), hostMac, hostVlanId, false);
            ips.forEach(ip ->
                    processRoutingRule(location.deviceId(), location.port(), hostMac, hostVlanId, ip, false)
            );
        }

        // Use the pair link temporarily before the second location of a dual-homed host shows up.
        // This do not affect single-homed hosts since the flow will be blocked in
        // processBridgingRule or processRoutingRule due to VLAN or IP mismatch respectively
        srManager.getPairDeviceId(location.deviceId()).ifPresent(pairDeviceId -> {
            if (srManager.mastershipService.isLocalMaster(pairDeviceId) &&
                    host.locations().stream().noneMatch(l -> l.deviceId().equals(pairDeviceId))) {
                srManager.getPairLocalPorts(pairDeviceId).ifPresent(pairRemotePort -> {
                    // NOTE: Since the pairLocalPort is trunk port, use assigned vlan of original port
                    //       when the host is untagged
                    VlanId vlanId = Optional.ofNullable(srManager.getInternalVlanId(location)).orElse(hostVlanId);

                    processBridgingRule(pairDeviceId, pairRemotePort, hostMac, vlanId, false);
                    ips.forEach(ip -> processRoutingRule(pairDeviceId, pairRemotePort, hostMac, vlanId,
                                    ip, false));
                });
            }
        });
    }

    void processHostRemovedEvent(HostEvent event) {
        processHostRemoved(event.subject());
    }

    private void processHostRemoved(Host host) {
        MacAddress hostMac = host.mac();
        VlanId hostVlanId = host.vlan();
        Set<HostLocation> locations = host.locations();
        Set<IpAddress> ips = host.ipAddresses();
        log.info("Host {}/{} is removed from {}", hostMac, hostVlanId, locations);

        locations.forEach(location -> {
            if (srManager.isMasterOf(location)) {
                processBridgingRule(location.deviceId(), location.port(), hostMac, hostVlanId, true);
                ips.forEach(ip ->
                        processRoutingRule(location.deviceId(), location.port(), hostMac, hostVlanId, ip, true)
                );
            }

            // Also remove redirection flows on the pair device if exists.
            Optional<DeviceId> pairDeviceId = srManager.getPairDeviceId(location.deviceId());
            Optional<PortNumber> pairLocalPort = srManager.getPairLocalPorts(location.deviceId());
            if (pairDeviceId.isPresent() && pairLocalPort.isPresent() &&
                    srManager.mastershipService.isLocalMaster(pairDeviceId.get())) {
                // NOTE: Since the pairLocalPort is trunk port, use assigned vlan of original port
                //       when the host is untagged
                VlanId vlanId = Optional.ofNullable(srManager.getInternalVlanId(location)).orElse(hostVlanId);

                processBridgingRule(pairDeviceId.get(), pairLocalPort.get(), hostMac, vlanId, true);
                ips.forEach(ip ->
                        processRoutingRule(pairDeviceId.get(), pairLocalPort.get(), hostMac, vlanId,
                                ip, true));
            }
        });
    }

    void processHostMovedEvent(HostEvent event) {
        MacAddress hostMac = event.subject().mac();
        VlanId hostVlanId = event.subject().vlan();
        Set<HostLocation> prevLocations = event.prevSubject().locations();
        Set<IpAddress> prevIps = event.prevSubject().ipAddresses();
        Set<HostLocation> newLocations = event.subject().locations();
        Set<IpAddress> newIps = event.subject().ipAddresses();
        log.info("Host {}/{} is moved from {} to {}", hostMac, hostVlanId, prevLocations, newLocations);

        Set<DeviceId> newDeviceIds = newLocations.stream().map(HostLocation::deviceId)
                .collect(Collectors.toSet());

        // For each old location
        Sets.difference(prevLocations, newLocations).stream().filter(srManager::isMasterOf)
                .forEach(prevLocation -> {
            // Remove routing rules for old IPs
            Sets.difference(prevIps, newIps).forEach(ip ->
                    processRoutingRule(prevLocation.deviceId(), prevLocation.port(), hostMac, hostVlanId,
                            ip, true)
            );

            // Redirect the flows to pair link if configured
            // Note: Do not continue removing any rule
            Optional<DeviceId> pairDeviceId = srManager.getPairDeviceId(prevLocation.deviceId());
            Optional<PortNumber> pairLocalPort = srManager.getPairLocalPorts(prevLocation.deviceId());
            if (pairDeviceId.isPresent() && pairLocalPort.isPresent() && newLocations.stream()
                    .anyMatch(location -> location.deviceId().equals(pairDeviceId.get()))) {
                // NOTE: Since the pairLocalPort is trunk port, use assigned vlan of original port
                //       when the host is untagged
                VlanId vlanId = Optional.ofNullable(srManager.getInternalVlanId(prevLocation)).orElse(hostVlanId);

                processBridgingRule(prevLocation.deviceId(), pairLocalPort.get(), hostMac, vlanId, false);
                newIps.forEach(ip ->
                        processRoutingRule(prevLocation.deviceId(), pairLocalPort.get(), hostMac, vlanId,
                            ip, false));
                return;
            }

            // Remove bridging rule and routing rules for unchanged IPs if the host moves from a switch to another.
            // Otherwise, do not remove and let the adding part update the old flow
            if (!newDeviceIds.contains(prevLocation.deviceId())) {
                processBridgingRule(prevLocation.deviceId(), prevLocation.port(), hostMac, hostVlanId, true);
                Sets.intersection(prevIps, newIps).forEach(ip ->
                        processRoutingRule(prevLocation.deviceId(), prevLocation.port(), hostMac, hostVlanId,
                                ip, true)
                );
            }

            // Remove bridging rules if new interface vlan is different from old interface vlan
            // Otherwise, do not remove and let the adding part update the old flow
            if (newLocations.stream().noneMatch(newLocation -> {
                VlanId oldAssignedVlan = srManager.getInternalVlanId(prevLocation);
                VlanId newAssignedVlan = srManager.getInternalVlanId(newLocation);
                // Host is tagged and the new location has the host vlan in vlan-tagged
                return srManager.getTaggedVlanId(newLocation).contains(hostVlanId) ||
                        (oldAssignedVlan != null && newAssignedVlan != null &&
                        // Host is untagged and the new location has the same assigned vlan
                        oldAssignedVlan.equals(newAssignedVlan));
            })) {
                processBridgingRule(prevLocation.deviceId(), prevLocation.port(), hostMac, hostVlanId, true);
            }

            // Remove routing rules for unchanged IPs if none of the subnet of new location contains
            // the IP. Otherwise, do not remove and let the adding part update the old flow
            Sets.intersection(prevIps, newIps).forEach(ip -> {
                if (newLocations.stream().noneMatch(newLocation ->
                        srManager.deviceConfiguration.inSameSubnet(newLocation, ip))) {
                    processRoutingRule(prevLocation.deviceId(), prevLocation.port(), hostMac, hostVlanId,
                            ip, true);
                }
            });
        });

        // For each new location, add all new IPs.
        Sets.difference(newLocations, prevLocations).stream().filter(srManager::isMasterOf)
                .forEach(newLocation -> {
            processBridgingRule(newLocation.deviceId(), newLocation.port(), hostMac, hostVlanId, false);
            newIps.forEach(ip ->
                    processRoutingRule(newLocation.deviceId(), newLocation.port(), hostMac, hostVlanId,
                            ip, false)
            );
        });

        // For each unchanged location, add new IPs and remove old IPs.
        Sets.intersection(newLocations, prevLocations).stream().filter(srManager::isMasterOf)
                .forEach(unchangedLocation -> {
            Sets.difference(prevIps, newIps).forEach(ip ->
                    processRoutingRule(unchangedLocation.deviceId(), unchangedLocation.port(), hostMac,
                            hostVlanId, ip, true)
            );

            Sets.difference(newIps, prevIps).forEach(ip ->
                    processRoutingRule(unchangedLocation.deviceId(), unchangedLocation.port(), hostMac,
                        hostVlanId, ip, false)
            );
        });
    }

    void processHostUpdatedEvent(HostEvent event) {
        MacAddress mac = event.subject().mac();
        VlanId vlanId = event.subject().vlan();
        Set<HostLocation> locations = event.subject().locations();
        Set<IpAddress> prevIps = event.prevSubject().ipAddresses();
        Set<IpAddress> newIps = event.subject().ipAddresses();
        log.info("Host {}/{} is updated", mac, vlanId);

        locations.stream().filter(srManager::isMasterOf).forEach(location -> {
            Sets.difference(prevIps, newIps).forEach(ip ->
                    processRoutingRule(location.deviceId(), location.port(), mac, vlanId, ip, true)
            );
            Sets.difference(newIps, prevIps).forEach(ip ->
                    processRoutingRule(location.deviceId(), location.port(), mac, vlanId, ip, false)
            );
        });
    }

    /**
     * Generates a forwarding objective builder for bridging rules.
     * <p>
     * The forwarding objective bridges packets destined to a given MAC to
     * given port on given device.
     *
     * @param deviceId Device that host attaches to
     * @param mac MAC address of the host
     * @param hostVlanId VLAN ID of the host
     * @param outport Port that host attaches to
     * @param revoke true if forwarding objective is meant to revoke forwarding rule
     * @return Forwarding objective builder
     */
    ForwardingObjective.Builder bridgingFwdObjBuilder(
            DeviceId deviceId, MacAddress mac, VlanId hostVlanId,
            PortNumber outport, boolean revoke) {
        ConnectPoint connectPoint = new ConnectPoint(deviceId, outport);
        VlanId untaggedVlan = srManager.getUntaggedVlanId(connectPoint);
        Set<VlanId> taggedVlans = srManager.getTaggedVlanId(connectPoint);
        VlanId nativeVlan = srManager.getNativeVlanId(connectPoint);

        // Create host selector
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        sbuilder.matchEthDst(mac);

        // Create host treatment
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        tbuilder.immediate().setOutput(outport);

        // Create host meta
        TrafficSelector.Builder mbuilder = DefaultTrafficSelector.builder();

        // Adjust the selector, treatment and meta according to VLAN configuration
        if (taggedVlans.contains(hostVlanId)) {
            sbuilder.matchVlanId(hostVlanId);
            mbuilder.matchVlanId(hostVlanId);
        } else if (hostVlanId.equals(VlanId.NONE)) {
            if (untaggedVlan != null) {
                sbuilder.matchVlanId(untaggedVlan);
                mbuilder.matchVlanId(untaggedVlan);
                tbuilder.immediate().popVlan();
            } else if (nativeVlan != null) {
                sbuilder.matchVlanId(nativeVlan);
                mbuilder.matchVlanId(nativeVlan);
                tbuilder.immediate().popVlan();
            } else {
                log.warn("Untagged host {}/{} is not allowed on {} without untagged or native" +
                        "vlan config", mac, hostVlanId, connectPoint);
                return null;
            }
        } else {
            log.warn("Tagged host {}/{} is not allowed on {} without VLAN listed in tagged vlan",
                    mac, hostVlanId, connectPoint);
            return null;
        }

        // All forwarding is via Groups. Drivers can re-purpose to flow-actions if needed.
        // If the objective is to revoke an existing rule, and for some reason
        // the next-objective does not exist, then a new one should not be created
        int portNextObjId = srManager.getPortNextObjectiveId(deviceId, outport,
                tbuilder.build(), mbuilder.build(), !revoke);
        if (portNextObjId == -1) {
            // Warning log will come from getPortNextObjective method
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
     * Populate or revoke a bridging rule on given deviceId that matches given mac, given vlan and
     * output to given port.
     *
     * @param deviceId device ID
     * @param port port
     * @param mac mac address
     * @param vlanId VLAN ID
     * @param revoke true to revoke the rule; false to populate
     */
    private void processBridgingRule(DeviceId deviceId, PortNumber port, MacAddress mac,
                                     VlanId vlanId, boolean revoke) {
        log.debug("{} bridging entry for host {}/{} at {}:{}", revoke ? "Revoking" : "Populating",
                mac, vlanId, deviceId, port);

        ForwardingObjective.Builder fob = bridgingFwdObjBuilder(deviceId, mac, vlanId, port, revoke);
        if (fob == null) {
            log.warn("Fail to build fwd obj for host {}/{}. Abort.", mac, vlanId);
            return;
        }

        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Brigding rule for {}/{} {}", mac, vlanId,
                        revoke ? "revoked" : "populated"),
                (objective, error) -> log.warn("Failed to {} bridging rule for {}/{}: {}",
                        revoke ? "revoked" : "populated", mac, vlanId, error));
        flowObjectiveService.forward(deviceId, revoke ? fob.remove(context) : fob.add(context));
    }

    /**
     * Populate or revoke a routing rule on given deviceId that matches given ip,
     * set destination mac to given mac, set vlan to given vlan and output to given port.
     *
     * @param deviceId device ID
     * @param port port
     * @param mac mac address
     * @param vlanId VLAN ID
     * @param ip IP address
     * @param revoke true to revoke the rule; false to populate
     */
    private void processRoutingRule(DeviceId deviceId, PortNumber port, MacAddress mac,
                                    VlanId vlanId, IpAddress ip, boolean revoke) {
        ConnectPoint location = new ConnectPoint(deviceId, port);
        if (!srManager.deviceConfiguration.inSameSubnet(location, ip)) {
            log.info("{} is not included in the subnet config of {}/{}. Ignored.", ip, deviceId, port);
            return;
        }

        log.info("{} routing rule for {} at {}", revoke ? "Revoking" : "Populating", ip, location);
        if (revoke) {
            srManager.defaultRoutingHandler.revokeRoute(deviceId, ip.toIpPrefix(), mac, vlanId, port);
        } else {
            srManager.defaultRoutingHandler.populateRoute(deviceId, ip.toIpPrefix(), mac, vlanId, port);
        }
    }
}
