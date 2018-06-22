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

import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.ProbeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import java.util.HashSet;
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

    /**
     * Constructs the HostHandler.
     *
     * @param srManager Segment Routing manager
     */
    HostHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        hostService = srManager.hostService;
    }

    protected void init(DeviceId devId) {
        hostService.getHosts().forEach(host ->
            host.locations().stream()
                    .filter(location -> location.deviceId().equals(devId) ||
                            location.deviceId().equals(srManager.getPairDeviceId(devId).orElse(null)))
                    .forEach(location -> processHostAddedAtLocation(host, location))
        );
    }

    void processHostAddedEvent(HostEvent event) {
        processHostAdded(event.subject());
    }

    private void processHostAdded(Host host) {
        host.locations().forEach(location -> processHostAddedAtLocation(host, location));
        // ensure dual-homed host locations have viable uplinks
        if (host.locations().size() > 1 || srManager.singleHomedDown) {
            host.locations().forEach(loc -> {
                if (srManager.mastershipService.isLocalMaster(loc.deviceId())) {
                    srManager.linkHandler.checkUplinksForHost(loc);
                }
            });
        }
    }

    void processHostAddedAtLocation(Host host, HostLocation location) {
        checkArgument(host.locations().contains(location), "{} is not a location of {}", location, host);

        MacAddress hostMac = host.mac();
        VlanId hostVlanId = host.vlan();
        Set<HostLocation> locations = host.locations();
        Set<IpAddress> ips = host.ipAddresses();
        log.info("Host {}/{} is added at {}", hostMac, hostVlanId, locations);

        if (isDoubleTaggedHost(host)) {
            ips.forEach(ip ->
                processDoubleTaggedRoutingRule(location.deviceId(), location.port(), hostMac,
                                               host.innerVlan(), hostVlanId, host.tpid(), ip, false)
            );
        } else {
            processBridgingRule(location.deviceId(), location.port(), hostMac, hostVlanId, false);
            ips.forEach(ip ->
                processRoutingRule(location.deviceId(), location.port(), hostMac, hostVlanId, ip, false)
            );
        }

        // Use the pair link temporarily before the second location of a dual-homed host shows up.
        // This do not affect single-homed hosts since the flow will be blocked in
        // processBridgingRule or processRoutingRule due to VLAN or IP mismatch respectively
        srManager.getPairDeviceId(location.deviceId()).ifPresent(pairDeviceId -> {
            if (host.locations().stream().noneMatch(l -> l.deviceId().equals(pairDeviceId))) {
                srManager.getPairLocalPort(pairDeviceId).ifPresent(pairRemotePort -> {
                    // NOTE: Since the pairLocalPort is trunk port, use assigned vlan of original port
                    //       when the host is untagged
                    VlanId vlanId = vlanForPairPort(hostVlanId, location);
                    if (vlanId == null) {
                        return;
                    }

                    processBridgingRule(pairDeviceId, pairRemotePort, hostMac, vlanId, false);
                    ips.forEach(ip -> processRoutingRule(pairDeviceId, pairRemotePort, hostMac, vlanId,
                                    ip, false));

                    if (srManager.activeProbing) {
                        probe(host, location, pairDeviceId, pairRemotePort);
                    }
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
            if (isDoubleTaggedHost(host)) {
                ips.forEach(ip ->
                    processDoubleTaggedRoutingRule(location.deviceId(), location.port(), hostMac,
                                                   host.innerVlan(), hostVlanId, host.tpid(), ip, true)
                );
            } else {
                processBridgingRule(location.deviceId(), location.port(), hostMac, hostVlanId, true);
                ips.forEach(ip ->
                    processRoutingRule(location.deviceId(), location.port(), hostMac, hostVlanId, ip, true)
                );
            }

            // Also remove redirection flows on the pair device if exists.
            Optional<DeviceId> pairDeviceId = srManager.getPairDeviceId(location.deviceId());
            Optional<PortNumber> pairLocalPort = srManager.getPairLocalPort(location.deviceId());
            if (pairDeviceId.isPresent() && pairLocalPort.isPresent()) {
                // NOTE: Since the pairLocalPort is trunk port, use assigned vlan of original port
                //       when the host is untagged
                VlanId vlanId = vlanForPairPort(hostVlanId, location);
                if (vlanId == null) {
                    return;
                }

                processBridgingRule(pairDeviceId.get(), pairLocalPort.get(), hostMac, vlanId, true);
                ips.forEach(ip ->
                        processRoutingRule(pairDeviceId.get(), pairLocalPort.get(), hostMac, vlanId,
                                ip, true));
            }

            // Delete prefix from sr-device-subnet when the next hop host is removed
            srManager.routeService.getRouteTables().forEach(tableId -> {
                srManager.routeService.getRoutes(tableId).forEach(routeInfo -> {
                    if (routeInfo.allRoutes().stream().anyMatch(rr -> ips.contains(rr.nextHop()))) {
                        log.debug("HostRemoved. removeSubnet {}, {}", location, routeInfo.prefix());
                        srManager.deviceConfiguration.removeSubnet(location, routeInfo.prefix());
                    }
                });
            });
        });
    }

    void processHostMovedEvent(HostEvent event) {
        Host host = event.subject();
        MacAddress hostMac = host.mac();
        VlanId hostVlanId = host.vlan();
        Set<HostLocation> prevLocations = event.prevSubject().locations();
        Set<IpAddress> prevIps = event.prevSubject().ipAddresses();
        Set<HostLocation> newLocations = host.locations();
        Set<IpAddress> newIps = host.ipAddresses();
        EthType hostTpid = host.tpid();
        boolean doubleTaggedHost = isDoubleTaggedHost(host);

        log.info("Host {}/{} is moved from {} to {}", hostMac, hostVlanId, prevLocations, newLocations);
        Set<DeviceId> newDeviceIds = newLocations.stream().map(HostLocation::deviceId)
                .collect(Collectors.toSet());

        // For each old location
        Sets.difference(prevLocations, newLocations).forEach(prevLocation -> {
            // First of all, verify each old location
            srManager.probingService.probeHost(host, prevLocation, ProbeMode.VERIFY);

            // Remove routing rules for old IPs
            Sets.difference(prevIps, newIps).forEach(ip -> {
                if (doubleTaggedHost) {
                    processDoubleTaggedRoutingRule(prevLocation.deviceId(), prevLocation.port(),
                                                   hostMac, host.innerVlan(), hostVlanId, hostTpid, ip, true);
                } else {
                    processRoutingRule(prevLocation.deviceId(), prevLocation.port(), hostMac, hostVlanId,
                                       ip, true);
                }
            });

            // Redirect the flows to pair link if configured
            // Note: Do not continue removing any rule
            Optional<DeviceId> pairDeviceId = srManager.getPairDeviceId(prevLocation.deviceId());
            Optional<PortNumber> pairLocalPort = srManager.getPairLocalPort(prevLocation.deviceId());
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

            // Remove flows for unchanged IPs only when the host moves from a switch to another.
            // Otherwise, do not remove and let the adding part update the old flow
            if (!newDeviceIds.contains(prevLocation.deviceId())) {
                processBridgingRule(prevLocation.deviceId(), prevLocation.port(), hostMac, hostVlanId, true);
                Sets.intersection(prevIps, newIps).forEach(ip -> {
                    if (doubleTaggedHost) {
                        processDoubleTaggedRoutingRule(prevLocation.deviceId(), prevLocation.port(),
                                                       hostMac, host.innerVlan(), hostVlanId, hostTpid, ip, true);
                    } else {
                        processRoutingRule(prevLocation.deviceId(), prevLocation.port(),
                                           hostMac, hostVlanId, ip, true);
                    }
                });
            }

            // Remove bridging rules if new interface vlan is different from old interface vlan
            // Otherwise, do not remove and let the adding part update the old flow
            if (newLocations.stream().noneMatch(newLocation -> {
                VlanId oldAssignedVlan = srManager.getInternalVlanId(prevLocation);
                VlanId newAssignedVlan = srManager.getInternalVlanId(newLocation);
                // Host is tagged and the new location has the host vlan in vlan-tagged
                return srManager.interfaceService.getTaggedVlanId(newLocation).contains(hostVlanId) ||
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
                    if (doubleTaggedHost) {
                        processDoubleTaggedRoutingRule(prevLocation.deviceId(), prevLocation.port(),
                                                       hostMac, host.innerVlan(), hostVlanId, hostTpid, ip, true);
                    } else {
                        processRoutingRule(prevLocation.deviceId(), prevLocation.port(),
                                           hostMac, hostVlanId, ip, true);
                    }
                }
            });
        });

        // For each new location, add all new IPs.
        Sets.difference(newLocations, prevLocations).forEach(newLocation -> {
            processBridgingRule(newLocation.deviceId(), newLocation.port(), hostMac, hostVlanId, false);
            newIps.forEach(ip -> {
                if (doubleTaggedHost) {
                    processDoubleTaggedRoutingRule(newLocation.deviceId(), newLocation.port(),
                                                   hostMac, host.innerVlan(), hostVlanId, hostTpid, ip, false);
                } else {
                    processRoutingRule(newLocation.deviceId(), newLocation.port(), hostMac, hostVlanId,
                                       ip, false);
                }
            });

            // Probe on pair device when host move
            // Majorly for the 2nd step of [1A/x, 1B/x] -> [1A/x, 1B/y] -> [1A/y, 1B/y]
            // But will also cover [1A/x] -> [1A/y] -> [1A/y, 1B/y]
            if (srManager.activeProbing) {
                srManager.getPairDeviceId(newLocation.deviceId()).ifPresent(pairDeviceId ->
                        srManager.getPairLocalPort(pairDeviceId).ifPresent(pairRemotePort ->
                                probe(host, newLocation, pairDeviceId, pairRemotePort)
                        )
                );
            }
        });

        // For each unchanged location, add new IPs and remove old IPs.
        Sets.intersection(newLocations, prevLocations).forEach(unchangedLocation -> {
            Sets.difference(prevIps, newIps).forEach(ip -> {
                 if (doubleTaggedHost) {
                     processDoubleTaggedRoutingRule(unchangedLocation.deviceId(), unchangedLocation.port(),
                                                    hostMac, host.innerVlan(), hostVlanId, hostTpid, ip, true);
                 } else {
                     processRoutingRule(unchangedLocation.deviceId(), unchangedLocation.port(),
                                        hostMac, hostVlanId, ip, true);
                 }
            });

            Sets.difference(newIps, prevIps).forEach(ip -> {
                if (doubleTaggedHost) {
                    processDoubleTaggedRoutingRule(unchangedLocation.deviceId(), unchangedLocation.port(),
                                                   hostMac, host.innerVlan(), hostVlanId, hostTpid, ip, false);
                } else {
                    processRoutingRule(unchangedLocation.deviceId(), unchangedLocation.port(),
                                       hostMac, hostVlanId, ip, false);
                }
            });
        });

        // ensure dual-homed host locations have viable uplinks
        if (newLocations.size() > prevLocations.size() || srManager.singleHomedDown) {
            newLocations.forEach(loc -> {
                if (srManager.mastershipService.isLocalMaster(loc.deviceId())) {
                    srManager.linkHandler.checkUplinksForHost(loc);
                }
            });
        }
    }

    void processHostUpdatedEvent(HostEvent event) {
        Host host = event.subject();
        MacAddress hostMac = host.mac();
        VlanId hostVlanId = host.vlan();
        EthType hostTpid = host.tpid();
        Set<HostLocation> locations = host.locations();
        Set<IpAddress> prevIps = event.prevSubject().ipAddresses();
        Set<IpAddress> newIps = host.ipAddresses();
        log.info("Host {}/{} is updated", hostMac, hostVlanId);

        locations.forEach(location -> {
            Sets.difference(prevIps, newIps).forEach(ip -> {
                if (isDoubleTaggedHost(host)) {
                    processDoubleTaggedRoutingRule(location.deviceId(), location.port(), hostMac,
                                                   host.innerVlan(), hostVlanId, hostTpid, ip, true);
                } else {
                    processRoutingRule(location.deviceId(), location.port(), hostMac,
                                       hostVlanId, ip, true);
                }
            });
            Sets.difference(newIps, prevIps).forEach(ip -> {
                if (isDoubleTaggedHost(host)) {
                    processDoubleTaggedRoutingRule(location.deviceId(), location.port(), hostMac,
                                                   host.innerVlan(), hostVlanId, hostTpid, ip, false);
                } else {
                    processRoutingRule(location.deviceId(), location.port(), hostMac,
                                       hostVlanId, ip, false);
                }
            });
        });

        // Use the pair link temporarily before the second location of a dual-homed host shows up.
        // This do not affect single-homed hosts since the flow will be blocked in
        // processBridgingRule or processRoutingRule due to VLAN or IP mismatch respectively
        locations.forEach(location ->
            srManager.getPairDeviceId(location.deviceId()).ifPresent(pairDeviceId -> {
                if (locations.stream().noneMatch(l -> l.deviceId().equals(pairDeviceId))) {
                    Set<IpAddress> ipsToAdd = Sets.difference(newIps, prevIps);
                    Set<IpAddress> ipsToRemove = Sets.difference(prevIps, newIps);

                    srManager.getPairLocalPort(pairDeviceId).ifPresent(pairRemotePort -> {
                        // NOTE: Since the pairLocalPort is trunk port, use assigned vlan of original port
                        //       when the host is untagged
                        VlanId vlanId = vlanForPairPort(hostVlanId, location);
                        if (vlanId == null) {
                            return;
                        }

                        ipsToRemove.forEach(ip ->
                                processRoutingRule(pairDeviceId, pairRemotePort, hostMac, vlanId, ip, true)
                        );
                        ipsToAdd.forEach(ip ->
                                processRoutingRule(pairDeviceId, pairRemotePort, hostMac, vlanId, ip, false)
                        );

                        if (srManager.activeProbing) {
                            probe(host, location, pairDeviceId, pairRemotePort);
                        }
                    });
                }
            })
        );
    }

    /**
     * When a non-pair port comes up, probe each host on the pair device if
     * (1) the host is tagged and the tagged vlan of current port contains host vlan; or
     * (2) the host is untagged and the internal vlan is the same on the host port and current port.
     *
     * @param cp connect point
     */
    void processPortUp(ConnectPoint cp) {
        if (cp.port().equals(srManager.getPairLocalPort(cp.deviceId()).orElse(null))) {
            return;
        }
        if (srManager.activeProbing) {
            srManager.getPairDeviceId(cp.deviceId())
                    .ifPresent(pairDeviceId -> srManager.hostService.getConnectedHosts(pairDeviceId).stream()
                            .filter(host -> isHostInVlanOfPort(host, pairDeviceId, cp))
                            .forEach(host -> srManager.probingService.probeHost(host, cp, ProbeMode.DISCOVER))
                    );
        }
    }

    /**
     * Checks if given host located on given device id matches VLAN config of current port.
     *
     * @param host host to check
     * @param deviceId device id to check
     * @param cp current connect point
     * @return true if the host located at deviceId matches the VLAN config on cp
     */
    private boolean isHostInVlanOfPort(Host host, DeviceId deviceId, ConnectPoint cp) {
        VlanId internalVlan = srManager.getInternalVlanId(cp);
        Set<VlanId> taggedVlan = srManager.interfaceService.getTaggedVlanId(cp);

        return taggedVlan.contains(host.vlan()) ||
                (internalVlan != null && host.locations().stream()
                        .filter(l -> l.deviceId().equals(deviceId))
                        .map(srManager::getInternalVlanId)
                        .anyMatch(internalVlan::equals));
    }

    /**
     * Send a probe on all locations with the same VLAN on pair device, excluding pair port.
     *
     * @param host host to probe
     * @param location newly discovered host location
     * @param pairDeviceId pair device id
     * @param pairRemotePort pair remote port
     */
    private void probe(Host host, ConnectPoint location, DeviceId pairDeviceId, PortNumber pairRemotePort) {
        VlanId vlanToProbe = host.vlan().equals(VlanId.NONE) ?
                srManager.getInternalVlanId(location) : host.vlan();
        srManager.interfaceService.getInterfaces().stream()
                .filter(i -> i.vlanTagged().contains(vlanToProbe) ||
                        i.vlanUntagged().equals(vlanToProbe) ||
                        i.vlanNative().equals(vlanToProbe))
                .filter(i -> i.connectPoint().deviceId().equals(pairDeviceId))
                .filter(i -> !i.connectPoint().port().equals(pairRemotePort))
                .forEach(i -> {
                    log.debug("Probing host {} on pair device {}", host.id(), i.connectPoint());
                    srManager.probingService.probeHost(host, i.connectPoint(), ProbeMode.DISCOVER);
                });
    }

    /**
     * Populates or revokes a bridging rule on given deviceId that matches given mac,
     * given vlan and output to given port.
     *
     * @param deviceId device ID
     * @param port port
     * @param mac mac address
     * @param vlanId VLAN ID
     * @param revoke true to revoke the rule; false to populate
     */
    private void processBridgingRule(DeviceId deviceId, PortNumber port, MacAddress mac,
                                     VlanId vlanId, boolean revoke) {
        log.info("{} bridging entry for host {}/{} at {}:{}", revoke ? "Revoking" : "Populating",
                mac, vlanId, deviceId, port);

        if (!revoke) {
            srManager.defaultRoutingHandler.populateBridging(deviceId, port, mac, vlanId);
        } else {
            srManager.defaultRoutingHandler.revokeBridging(deviceId, port, mac, vlanId);
        }
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

    /**
     * Populate or revoke a routing rule and egress rules on given deviceId that matches given IP,
     * to set destination mac to given mac, set inner vlan and outer vlan to given vlans,
     * set outer TPID, and output to given port.
     *
     * @param deviceId  device ID
     * @param port      port
     * @param mac       mac address
     * @param innerVlan inner VLAN ID
     * @param outerVlan outer VLAN ID
     * @param outerTpid outer TPID
     * @param ip        IP address
     * @param revoke    true to revoke the rule; false to populate
     */
    private void processDoubleTaggedRoutingRule(DeviceId deviceId, PortNumber port,
                                                MacAddress mac, VlanId innerVlan,
                                                VlanId outerVlan, EthType outerTpid,
                                                IpAddress ip, boolean revoke) {
        ConnectPoint location = new ConnectPoint(deviceId, port);
        if (!srManager.deviceConfiguration.inSameSubnet(location, ip)) {
            log.info("{} is not included in the subnet config of {}/{}. Ignored.", ip, deviceId, port);
            return;
        }
        log.info("{} routing rule for double-tagged host {} at {}",
                 revoke ? "Revoking" : "Populating", ip, location);
        if (revoke) {
            srManager.defaultRoutingHandler.revokeDoubleTaggedRoute(
                    deviceId, ip.toIpPrefix(), mac, innerVlan, outerVlan, outerTpid, port);
        } else {
            srManager.defaultRoutingHandler.populateDoubleTaggedRoute(
                    deviceId, ip.toIpPrefix(), mac, innerVlan, outerVlan, outerTpid, port);
        }
    }

    /**
     * Returns VLAN ID to be used to program redirection flow on pair port.
     *
     * @param hostVlanId host VLAN ID
     * @param location host location
     * @return VLAN ID to be used; Or null if host VLAN does not match the interface config
     */
    VlanId vlanForPairPort(VlanId hostVlanId, ConnectPoint location) {
        VlanId internalVlan = srManager.getInternalVlanId(location);
        Set<VlanId> taggedVlan = srManager.interfaceService.getTaggedVlanId(location);

        if (!hostVlanId.equals(VlanId.NONE) && taggedVlan.contains(hostVlanId)) {
            return hostVlanId;
        } else if (hostVlanId.equals(VlanId.NONE) && internalVlan != null) {
            return internalVlan;
        } else {
            log.warn("VLAN mismatch. hostVlan={}, location={}, internalVlan={}, taggedVlan={}",
                    hostVlanId, location, internalVlan, taggedVlan);
            return null;
        }
    }

    /**
     * Update forwarding objective for unicast bridging and unicast routing.
     * Also check the validity of updated interface configuration on VLAN.
     *
     * @param deviceId device ID that host attaches to
     * @param portNum port number that host attaches to
     * @param vlanId Vlan ID configured on the switch port
     * @param popVlan true to pop Vlan tag at TrafficTreatment, false otherwise
     * @param install true to populate the objective, false to revoke
     */
    void processIntfVlanUpdatedEvent(DeviceId deviceId, PortNumber portNum, VlanId vlanId,
                                 boolean popVlan, boolean install) {
        ConnectPoint connectPoint = new ConnectPoint(deviceId, portNum);
        Set<Host> hosts = hostService.getConnectedHosts(connectPoint);

        if (hosts == null || hosts.size() == 0) {
            log.debug("processIntfVlanUpdatedEvent: No hosts connected to {}", connectPoint);
            return;
        }

        hosts.forEach(host -> {
            MacAddress mac = host.mac();
            VlanId hostVlanId = host.vlan();

            // Check whether the host vlan is valid for new interface configuration
            if ((!popVlan && hostVlanId.equals(vlanId)) ||
                    (popVlan && hostVlanId.equals(VlanId.NONE))) {
                srManager.defaultRoutingHandler.updateBridging(deviceId, portNum, mac, vlanId, popVlan, install);
                // Update Forwarding objective and corresponding simple Next objective
                // for each host and IP address connected to given port
                host.ipAddresses().forEach(ipAddress ->
                    srManager.defaultRoutingHandler.updateFwdObj(deviceId, portNum, ipAddress.toIpPrefix(),
                                                                mac, vlanId, popVlan, install)
                );
            }
        });
    }

    /**
     * Populate or revoke routing rule for each host, according to the updated
     * subnet configuration on the interface.
     * @param cp connect point of the updated interface
     * @param ipPrefixSet IP Prefixes added or removed
     * @param install true if IP Prefixes added, false otherwise
     */
    void processIntfIpUpdatedEvent(ConnectPoint cp, Set<IpPrefix> ipPrefixSet, boolean install) {
        Set<Host> hosts = hostService.getConnectedHosts(cp);

        if (hosts == null || hosts.size() == 0) {
            log.debug("processIntfIpUpdatedEvent: No hosts connected to {}", cp);
            return;
        }

        // Check whether the host IP address is in the interface's subnet
        hosts.forEach(host ->
            host.ipAddresses().forEach(hostIpAddress -> {
                ipPrefixSet.forEach(ipPrefix -> {
                    if (install && ipPrefix.contains(hostIpAddress)) {
                            srManager.defaultRoutingHandler.populateRoute(cp.deviceId(), hostIpAddress.toIpPrefix(),
                                                                         host.mac(), host.vlan(), cp.port());
                    } else if (!install && ipPrefix.contains(hostIpAddress)) {
                            srManager.defaultRoutingHandler.revokeRoute(cp.deviceId(), hostIpAddress.toIpPrefix(),
                                                                       host.mac(), host.vlan(), cp.port());
                    }
                });
            }));
    }

    /**
     * Returns the set of portnumbers on the given device that are part of the
     * locations for dual-homed hosts.
     *
     * @param deviceId the given deviceId
     * @return set of port numbers on given device that are dual-homed host
     *         locations. May be empty if no dual homed hosts are connected to
     *         the given device
     */
    Set<PortNumber> getDualHomedHostPorts(DeviceId deviceId) {
        Set<PortNumber> dualHomedLocations = new HashSet<>();
        srManager.hostService.getConnectedHosts(deviceId).stream()
            .filter(host -> host.locations().size() == 2)
            .forEach(host -> host.locations().stream()
                     .filter(loc -> loc.deviceId().equals(deviceId))
                        .forEach(loc -> dualHomedLocations.add(loc.port())));
        return dualHomedLocations;
    }

    /**
     * Checks if the given host is double-tagged or not.
     *
     * @param host host to check
     * @return true if it is double-tagged, false otherwise
     */
    private boolean isDoubleTaggedHost(Host host) {
        return !host.innerVlan().equals(VlanId.NONE);
    }

}
