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

import com.google.common.collect.Sets;
import java.util.Set;

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
    public HostHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        hostService = srManager.hostService;
        flowObjectiveService = srManager.flowObjectiveService;
    }

    protected void init(DeviceId devId) {
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
        log.info("Host {}/{} is added at {}:{}", mac, vlanId, deviceId, port);

        if (accepted(host)) {
            processBridgingRule(deviceId, port, mac, vlanId, false);
            ips.forEach(ip -> {
                processRoutingRule(deviceId, port, mac, vlanId, ip, false);
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
        log.info("Host {}/{} is removed from {}:{}", mac, vlanId, deviceId, port);

        if (accepted(host)) {
            processBridgingRule(deviceId, port, mac, vlanId, true);
            ips.forEach(ip -> {
                processRoutingRule(deviceId, port, mac, vlanId, ip, true);
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
        log.info("Host {}/{} is moved from {}:{} to {}:{}",
                mac, vlanId, prevDeviceId, prevPort, newDeviceId, newPort);

        if (accepted(event.prevSubject())) {
            processBridgingRule(prevDeviceId, prevPort, mac, vlanId, true);
            prevIps.forEach(ip -> {
                processRoutingRule(prevDeviceId, prevPort, mac, vlanId, ip, true);
            });
        }

        if (accepted(event.subject())) {
            processBridgingRule(newDeviceId, newPort, mac, vlanId, false);
            newIps.forEach(ip -> {
                processRoutingRule(newDeviceId, newPort, mac, vlanId, ip, false);
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
        log.info("Host {}/{} is updated", mac, vlanId);

        if (accepted(event.prevSubject())) {
            // Revoke previous IP table entry
            Sets.difference(prevIps, newIps).forEach(ip -> {
                processRoutingRule(prevDeviceId, prevPort, mac, vlanId, ip, true);
            });
        }

        if (accepted(event.subject())) {
            // Populate new IP table entry
            Sets.difference(newIps, prevIps).forEach(ip -> {
                processRoutingRule(newDeviceId, newPort, mac, vlanId, ip, false);
            });
        }
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
    private ForwardingObjective.Builder bridgingFwdObjBuilder(
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
        if (srManager.deviceConfiguration.inSameSubnet(location, ip)) {
            log.info("{} routing rule for {} at {}", revoke ? "Revoking" : "Populating",
                    ip, location);
            if (revoke) {
                srManager.routingRulePopulator.revokeRoute(deviceId, ip.toIpPrefix(), mac, vlanId, port);
            } else {
                srManager.routingRulePopulator.populateRoute(deviceId, ip.toIpPrefix(), mac, vlanId, port);
            }
        }
    }

    /**
     * Determines whether a host should be accepted by SR or not.
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
