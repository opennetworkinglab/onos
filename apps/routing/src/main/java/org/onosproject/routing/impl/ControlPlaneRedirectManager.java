/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.routing.impl;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.EthType;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.app.ApplicationService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceEvent;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.RouterConfig;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages connectivity between peers redirecting control traffic to a routing
 * control plane available on the dataplane.
 */
@Component(immediate = true, enabled = false)
public class ControlPlaneRedirectManager {

    private final Logger log = getLogger(getClass());

    private static final int MIN_IP_PRIORITY = 10;
    private static final int ACL_PRIORITY = 40001;
    private static final int OSPF_IP_PROTO = 0x59;

    private static final String APP_NAME = "org.onosproject.vrouter";
    private ApplicationId appId;

    private ConnectPoint controlPlaneConnectPoint;
    private boolean ospfEnabled = false;
    private List<String> interfaces = Collections.emptyList();
    private Map<Host, Set<Integer>> peerNextId = Maps.newConcurrentMap();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationService applicationService;

    private final InternalDeviceListener deviceListener = new InternalDeviceListener();
    private final InternalNetworkConfigListener networkConfigListener =
            new InternalNetworkConfigListener();
    private final InternalHostListener hostListener = new InternalHostListener();
    private final InternalInterfaceListener interfaceListener = new InternalInterfaceListener();

    @Activate
    public void activate() {
        this.appId = coreService.registerApplication(APP_NAME);

        deviceService.addListener(deviceListener);
        networkConfigService.addListener(networkConfigListener);
        hostService.addListener(hostListener);
        interfaceService.addListener(interfaceListener);

        readConfig();
        applicationService.registerDeactivateHook(this.appId, () -> provisionDevice(false));
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        networkConfigService.removeListener(networkConfigListener);
        hostService.removeListener(hostListener);
        interfaceService.removeListener(interfaceListener);
    }

    /**
     * Installs or removes interface configuration
     * based on the flag used on activate or deactivate.
     *
     **/
    private void readConfig() {
        ApplicationId routingAppId =
                coreService.registerApplication(RoutingService.ROUTER_APP_ID);

        RouterConfig config = networkConfigService.getConfig(
                routingAppId, RoutingService.ROUTER_CONFIG_CLASS);

        if (config == null) {
            log.warn("Router config not available");
            return;
        }

        controlPlaneConnectPoint = config.getControlPlaneConnectPoint();
        ospfEnabled = config.getOspfEnabled();
        interfaces = config.getInterfaces();

        provisionDevice(true);
    }

    /**
     * Installs or removes interface configuration for each interface
     * based on the flag used on activate or deactivate.
     *
     * @param install true to install flows, false to remove them
     **/
    private void provisionDevice(boolean install) {
        if (controlPlaneConnectPoint != null &&
                deviceService.isAvailable(controlPlaneConnectPoint.deviceId())) {
            DeviceId deviceId = controlPlaneConnectPoint.deviceId();

            interfaceService.getInterfaces().stream()
                    .filter(intf -> intf.connectPoint().deviceId().equals(deviceId))
                    .filter(intf -> interfaces.isEmpty() || interfaces.contains(intf.name()))
                    .forEach(intf -> provisionInterface(intf, install));

            log.info("Set up interfaces on {}", controlPlaneConnectPoint.deviceId());
        }
    }

    private void provisionInterface(Interface intf, boolean install) {
        updateInterfaceForwarding(intf, install);
        updateOspfForwarding(intf, install);
    }

    /**
     * Installs or removes the basic forwarding flows for each interface
     * based on the flag used.
     *
     * @param intf the Interface on which event is received
     * @param install true to create an add objective, false to create a remove
     *            objective
     **/
    private void updateInterfaceForwarding(Interface intf, boolean install) {
        log.debug("Adding interface objectives for {}", intf);

        DeviceId deviceId = controlPlaneConnectPoint.deviceId();
        PortNumber controlPlanePort = controlPlaneConnectPoint.port();
        for (InterfaceIpAddress ip : intf.ipAddresses()) {
            // create nextObjectives for forwarding to this interface and the
            // controlPlaneConnectPoint
            int cpNextId, intfNextId;
            if (intf.vlan() == VlanId.NONE) {
                cpNextId = modifyNextObjective(deviceId, controlPlanePort,
                               VlanId.vlanId(SingleSwitchFibInstaller.ASSIGNED_VLAN),
                               true, install);
                intfNextId = modifyNextObjective(deviceId, intf.connectPoint().port(),
                               VlanId.vlanId(SingleSwitchFibInstaller.ASSIGNED_VLAN),
                               true, install);
            } else {
                cpNextId = modifyNextObjective(deviceId, controlPlanePort,
                                               intf.vlan(), false, install);
                intfNextId = modifyNextObjective(deviceId, intf.connectPoint().port(),
                                                 intf.vlan(), false, install);
            }

            // IPv4 to router
            TrafficSelector toSelector = DefaultTrafficSelector.builder()
                    .matchInPort(intf.connectPoint().port())
                    .matchEthDst(intf.mac())
                    .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
                    .matchVlanId(intf.vlan())
                    .matchIPDst(ip.ipAddress().toIpPrefix())
                    .build();

            flowObjectiveService.forward(deviceId,
                    buildForwardingObjective(toSelector, null, cpNextId, install));

            // IPv4 from router
            TrafficSelector fromSelector = DefaultTrafficSelector.builder()
                    .matchInPort(controlPlanePort)
                    .matchEthSrc(intf.mac())
                    .matchVlanId(intf.vlan())
                    .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
                    .matchIPSrc(ip.ipAddress().toIpPrefix())
                    .build();

            flowObjectiveService.forward(deviceId,
                    buildForwardingObjective(fromSelector, null, intfNextId, install));

            // ARP to router
            toSelector = DefaultTrafficSelector.builder()
                    .matchInPort(intf.connectPoint().port())
                    .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                    .matchVlanId(intf.vlan())
                    .build();

            TrafficTreatment puntTreatment = DefaultTrafficTreatment.builder()
                    .punt()
                    .build();

            flowObjectiveService.forward(deviceId,
                    buildForwardingObjective(toSelector, puntTreatment, cpNextId, install));

            // ARP from router
            fromSelector = DefaultTrafficSelector.builder()
                    .matchInPort(controlPlanePort)
                    .matchEthSrc(intf.mac())
                    .matchVlanId(intf.vlan())
                    .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                    .matchArpSpa(ip.ipAddress().getIp4Address())
                    .build();

            flowObjectiveService.forward(deviceId,
            buildForwardingObjective(fromSelector, puntTreatment, intfNextId, install));
        }
    }

    /**
     * Installs or removes OSPF forwarding rules.
     *
     * @param intf the interface on which event is received
     * @param install true to create an add objective, false to create a remove
     *            objective
     **/
    private void updateOspfForwarding(Interface intf, boolean install) {
        // OSPF to router
        TrafficSelector toSelector = DefaultTrafficSelector.builder()
                .matchInPort(intf.connectPoint().port())
                .matchEthType(EthType.EtherType.IPV4.ethType().toShort())
                .matchVlanId(intf.vlan())
                .matchIPProtocol((byte) OSPF_IP_PROTO)
                .build();

        // create nextObjectives for forwarding to the controlPlaneConnectPoint
        DeviceId deviceId = controlPlaneConnectPoint.deviceId();
        PortNumber controlPlanePort = controlPlaneConnectPoint.port();
        int cpNextId;
        if (intf.vlan() == VlanId.NONE) {
            cpNextId = modifyNextObjective(deviceId, controlPlanePort,
                           VlanId.vlanId(SingleSwitchFibInstaller.ASSIGNED_VLAN),
                           true, install);
        } else {
            cpNextId = modifyNextObjective(deviceId, controlPlanePort,
                                           intf.vlan(), false, install);
        }
        log.debug("OSPF flows intf:{} nextid:{}", intf, cpNextId);
        flowObjectiveService.forward(controlPlaneConnectPoint.deviceId(),
                buildForwardingObjective(toSelector, null, cpNextId, install ? ospfEnabled : install));
    }

    /**
     * Creates a next objective for forwarding to a port. Handles metadata for
     * some pipelines that require vlan information for egress port.
     *
     * @param deviceId the device on which the next objective is being created
     * @param portNumber the egress port
     * @param vlanId vlan information for egress port
     * @param popVlan if vlan tag should be popped or not
     * @param install true to create an add next objective, false to create a remove
     *            next objective
     * @return nextId of the next objective created
     */
    private int modifyNextObjective(DeviceId deviceId, PortNumber portNumber,
                                    VlanId vlanId, boolean popVlan, boolean install) {
        int nextId = flowObjectiveService.allocateNextId();
        NextObjective.Builder nextObjBuilder = DefaultNextObjective
                .builder().withId(nextId)
                .withType(NextObjective.Type.SIMPLE)
                .fromApp(appId);

        TrafficTreatment.Builder ttBuilder = DefaultTrafficTreatment.builder();
        if (popVlan) {
            ttBuilder.popVlan();
        }
        ttBuilder.setOutput(portNumber);

        // setup metadata to pass to nextObjective - indicate the vlan on egress
        // if needed by the switch pipeline.
        TrafficSelector.Builder metabuilder = DefaultTrafficSelector.builder();
        metabuilder.matchVlanId(vlanId);

        nextObjBuilder.withMeta(metabuilder.build());
        nextObjBuilder.addTreatment(ttBuilder.build());
        log.debug("Submitted next objective {} in device {} for port/vlan {}/{}",
                nextId, deviceId, portNumber, vlanId);
        if (install) {
             flowObjectiveService.next(deviceId, nextObjBuilder.add());
        } else {
             flowObjectiveService.next(deviceId, nextObjBuilder.remove());
        }
        return nextId;
    }

    /**
     * Builds a forwarding objective from the given selector, treatment and nextId.
     *
     * @param selector selector
     * @param treatment treatment to apply to packet, can be null
     * @param nextId next objective to point to for forwarding packet
     * @param add true to create an add objective, false to create a remove
     *            objective
     * @return forwarding objective
     */
    private ForwardingObjective buildForwardingObjective(TrafficSelector selector,
                                                         TrafficTreatment treatment,
                                                         int nextId,
                                                         boolean add) {
        DefaultForwardingObjective.Builder fobBuilder = DefaultForwardingObjective.builder();
        fobBuilder.withSelector(selector);
        if (treatment != null) {
            fobBuilder.withTreatment(treatment);
        }
        if (nextId != -1) {
            fobBuilder.nextStep(nextId);
        }
        fobBuilder.fromApp(appId)
            .withPriority(ACL_PRIORITY)
            .withFlag(ForwardingObjective.Flag.VERSATILE);

        return add ? fobBuilder.add() : fobBuilder.remove();
    }

    /**
     * Listener for device events.
     */
    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            if (controlPlaneConnectPoint != null &&
                    event.subject().id().equals(controlPlaneConnectPoint.deviceId())) {
                switch (event.type()) {
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                    if (deviceService.isAvailable(event.subject().id())) {
                        log.info("Device connected {}", event.subject().id());
                        provisionDevice(true);
                    }
                    break;
                case DEVICE_UPDATED:
                case DEVICE_REMOVED:
                case DEVICE_SUSPENDED:
                case PORT_ADDED:
                case PORT_UPDATED:
                case PORT_REMOVED:
                default:
                    break;
                }
            }
        }
    }

    /**
     * Listener for network config events.
     */
    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass().equals(RoutingService.ROUTER_CONFIG_CLASS)) {
                switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    readConfig();
                    break;
                case CONFIG_REGISTERED:
                case CONFIG_UNREGISTERED:
                case CONFIG_REMOVED:
                default:
                    break;
                }
            }
        }
    }

    /**
     * Listener for host events.
     */
    private class InternalHostListener implements HostListener {

        private void peerAdded(HostEvent event) {
            Host peer = event.subject();
            Optional<Interface> peerIntf =
                    interfaceService.getInterfacesByPort(peer.location()).stream()
                    .filter(intf -> interfaces.isEmpty() || interfaces.contains(intf.name()))
                    .filter(intf -> peer.vlan().equals(intf.vlan()))
                    .findFirst();
            if (!peerIntf.isPresent()) {
                log.debug("Adding peer {}/{} on {} but the interface is not configured",
                        peer.mac(), peer.vlan(), peer.location());
                return;
            }

            // Generate L3 Unicast groups and store it in the map
            int toRouterL3Unicast = createPeerGroup(peer.mac(), peerIntf.get().mac(),
                    peer.vlan(), peer.location().deviceId(), controlPlaneConnectPoint.port());
            int toPeerL3Unicast = createPeerGroup(peerIntf.get().mac(), peer.mac(),
                    peer.vlan(), peer.location().deviceId(), peer.location().port());
            peerNextId.put(peer, ImmutableSortedSet.of(toRouterL3Unicast, toPeerL3Unicast));

            // From peer to router
            peerIntf.get().ipAddresses().forEach(routerIp -> {
                flowObjectiveService.forward(peer.location().deviceId(),
                        createPeerObjBuilder(toRouterL3Unicast, routerIp.ipAddress().toIpPrefix()).add());
            });

            // From router to peer
            peer.ipAddresses().forEach(peerIp -> {
                flowObjectiveService.forward(peer.location().deviceId(),
                        createPeerObjBuilder(toPeerL3Unicast, peerIp.toIpPrefix()).add());
            });
        }

        private void peerRemoved(HostEvent event) {
            Host peer = event.subject();
            Optional<Interface> peerIntf =
                    interfaceService.getInterfacesByPort(peer.location()).stream()
                            .filter(intf -> interfaces.isEmpty() || interfaces.contains(intf.name()))
                            .filter(intf -> peer.vlan().equals(intf.vlan()))
                            .findFirst();
            if (!peerIntf.isPresent()) {
                log.debug("Removing peer {}/{} on {} but the interface is not configured",
                        peer.mac(), peer.vlan(), peer.location());
                return;
            }

            checkState(peerNextId.get(peer) != null,
                    "Peer nextId should not be null");
            checkState(peerNextId.get(peer).size() == 2,
                    "Wrong nextId associated with the peer");
            Iterator<Integer> iter = peerNextId.get(peer).iterator();
            int toRouterL3Unicast = iter.next();
            int toPeerL3Unicast = iter.next();

            // From peer to router
            peerIntf.get().ipAddresses().forEach(routerIp -> {
                flowObjectiveService.forward(peer.location().deviceId(),
                        createPeerObjBuilder(toRouterL3Unicast, routerIp.ipAddress().toIpPrefix()).remove());
            });

            // From router to peer
            peer.ipAddresses().forEach(peerIp -> {
                flowObjectiveService.forward(peer.location().deviceId(),
                        createPeerObjBuilder(toPeerL3Unicast, peerIp.toIpPrefix()).remove());
            });
        }

        private ForwardingObjective.Builder createPeerObjBuilder(
                int nextId, IpPrefix ipAddresses) {
            TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
            sbuilder.matchEthType(EthType.EtherType.IPV4.ethType().toShort());
            sbuilder.matchIPDst(ipAddresses);
            DefaultForwardingObjective.Builder builder =
                    DefaultForwardingObjective.builder()
                    .withSelector(sbuilder.build())
                    .fromApp(appId)
                    .withPriority(getPriorityFromPrefix(ipAddresses))
                    .withFlag(ForwardingObjective.Flag.SPECIFIC);
            if (nextId != -1) {
                builder.nextStep(nextId);
            }
            return builder;
        }

        private int createPeerGroup(MacAddress srcMac, MacAddress dstMac,
                VlanId vlanId, DeviceId deviceId, PortNumber port) {
            int nextId = flowObjectiveService.allocateNextId();
            NextObjective.Builder nextObjBuilder = DefaultNextObjective.builder()
                    .withId(nextId)
                    .withType(NextObjective.Type.SIMPLE)
                    .fromApp(appId);

            TrafficTreatment.Builder ttBuilder = DefaultTrafficTreatment.builder();
            ttBuilder.setEthSrc(srcMac);
            ttBuilder.setEthDst(dstMac);
            ttBuilder.setOutput(port);
            nextObjBuilder.addTreatment(ttBuilder.build());

            TrafficSelector.Builder metabuilder = DefaultTrafficSelector.builder();
            VlanId matchVlanId = (vlanId.equals(VlanId.NONE)) ?
                    VlanId.vlanId(SingleSwitchFibInstaller.ASSIGNED_VLAN) :
                    vlanId;
            metabuilder.matchVlanId(matchVlanId);
            nextObjBuilder.withMeta(metabuilder.build());

            flowObjectiveService.next(deviceId, nextObjBuilder.add());
            return nextId;
        }

        @Override
        public void event(HostEvent event) {
            DeviceId deviceId = event.subject().location().deviceId();
            if (!mastershipService.isLocalMaster(deviceId)) {
                return;
            }
            switch (event.type()) {
                case HOST_ADDED:
                    peerAdded(event);
                    break;
                case HOST_MOVED:
                    //TODO We assume BGP peer does not move for now
                    break;
                case HOST_REMOVED:
                    peerRemoved(event);
                    break;
                case HOST_UPDATED:
                    //TODO We assume BGP peer does not change IP for now
                    break;
                default:
                    break;
            }
        }
    }

    private int getPriorityFromPrefix(IpPrefix prefix) {
        return (prefix.isIp4()) ?
                2000 * prefix.prefixLength() + MIN_IP_PRIORITY :
                500 * prefix.prefixLength() + MIN_IP_PRIORITY;
    }

    /**
     * Update the flows comparing previous event and current event.
     *
     * @param prevIntf the previous interface event
     * @param intf the current occurred update event
     **/
    private void updateInterface(Interface prevIntf, Interface intf) {
        if (!prevIntf.vlan().equals(intf.vlan()) || !prevIntf.mac().equals(intf)) {
            provisionInterface(prevIntf, false);
            provisionInterface(intf, true);
        } else {
            List<InterfaceIpAddress> removeIps =
                    prevIntf.ipAddressesList().stream()
                    .filter(pre -> !intf.ipAddressesList().contains(pre))
                    .collect(Collectors.toList());
            List<InterfaceIpAddress> addIps =
                    intf.ipAddressesList().stream()
                    .filter(cur -> !prevIntf.ipAddressesList().contains(cur))
                    .collect(Collectors.toList());
            // removing flows with match parameters present in previous subject
            updateInterfaceForwarding(new Interface(prevIntf.name(), prevIntf.connectPoint(),
                    removeIps, prevIntf.mac(), prevIntf.vlan()), false);
            // adding flows with match parameters present in event subject
            updateInterfaceForwarding(new Interface(intf.name(), intf.connectPoint(),
                    addIps, intf.mac(), intf.vlan()), true);
        }
    }

    private class InternalInterfaceListener implements InterfaceListener {
        @Override
        public void event(InterfaceEvent event) {
            if (controlPlaneConnectPoint == null) {
                log.info("Control plane connect point is not configured. Abort InterfaceEvent.");
                return;
            }
            Interface intf = event.subject();
            Interface prevIntf = event.prevSubject();
            switch (event.type()) {
                case INTERFACE_ADDED:
                    if (intf != null && !intf.connectPoint().equals(controlPlaneConnectPoint)) {
                        provisionInterface(intf, true);
                    }
                    break;
                case INTERFACE_UPDATED:
                    if (intf != null && !intf.connectPoint().equals(controlPlaneConnectPoint)) {
                        updateInterface(prevIntf, intf);
                    }
                    break;
                case INTERFACE_REMOVED:
                    if (intf != null && !intf.connectPoint().equals(controlPlaneConnectPoint)) {
                        provisionInterface(intf, false);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
