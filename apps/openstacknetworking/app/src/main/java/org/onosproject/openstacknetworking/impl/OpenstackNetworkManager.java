/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkListener;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkStore;
import org.onosproject.openstacknetworking.api.OpenstackNetworkStoreDelegate;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.openstack4j.model.common.IdEntity;
import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.NetworkType;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknetworking.api.Constants.DIRECT;
import static org.onosproject.openstacknetworking.api.Constants.PCISLOT;
import static org.onosproject.openstacknetworking.api.Constants.portNamePrefixMap;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getIntfNameFromPciAddress;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfacing OpenStack network,
 * subnet, and port.
 */

@Service
@Component(immediate = true)
public class OpenstackNetworkManager
        extends ListenerRegistry<OpenstackNetworkEvent, OpenstackNetworkListener>
        implements OpenstackNetworkAdminService, OpenstackNetworkService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_NETWORK  = "OpenStack network %s %s";
    private static final String MSG_SUBNET  = "OpenStack subnet %s %s";
    private static final String MSG_PORT = "OpenStack port %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_NETWORK  = "OpenStack network cannot be null";
    private static final String ERR_NULL_NETWORK_ID  = "OpenStack network ID cannot be null";
    private static final String ERR_NULL_NETWORK_NAME  = "OpenStack network name cannot be null";
    private static final String ERR_NULL_SUBNET = "OpenStack subnet cannot be null";
    private static final String ERR_NULL_SUBNET_ID = "OpenStack subnet ID cannot be null";
    private static final String ERR_NULL_SUBNET_NET_ID = "OpenStack subnet network ID cannot be null";
    private static final String ERR_NULL_SUBNET_CIDR = "OpenStack subnet CIDR cannot be null";
    private static final String ERR_NULL_PORT = "OpenStack port cannot be null";
    private static final String ERR_NULL_PORT_ID = "OpenStack port ID cannot be null";
    private static final String ERR_NULL_PORT_NET_ID = "OpenStack port network ID cannot be null";

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_IN_USE = " still in use";
    private static final String ERR_DUPLICATE = " already exists";
    private static final String PORT_NAME_PREFIX_VM = "tap";

    private static final int PREFIX_LENGTH = 32;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkStore osNetworkStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    private final OpenstackNetworkStoreDelegate delegate = new InternalNetworkStoreDelegate();

    private ConsistentMap<String, ExternalPeerRouter> externalPeerRouterMap;

    private static final KryoNamespace SERIALIZER_EXTERNAL_PEER_ROUTER_MAP = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(ExternalPeerRouter.class)
            .register(DefaultExternalPeerRouter.class)
            .register(MacAddress.class)
            .register(IpAddress.class)
            .register(VlanId.class)
            .build();

    private ApplicationId appId;


    @Activate
    protected void activate() {
        appId = coreService.registerApplication(Constants.OPENSTACK_NETWORKING_APP_ID);

        osNetworkStore.setDelegate(delegate);
        log.info("Started");

        externalPeerRouterMap = storageService.<String, ExternalPeerRouter>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_EXTERNAL_PEER_ROUTER_MAP))
                .withName("external-routermap")
                .withApplicationId(appId)
                .build();
    }

    @Deactivate
    protected void deactivate() {
        osNetworkStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createNetwork(Network osNet) {
        checkNotNull(osNet, ERR_NULL_NETWORK);
        checkArgument(!Strings.isNullOrEmpty(osNet.getId()), ERR_NULL_NETWORK_ID);

        osNetworkStore.createNetwork(osNet);
        log.info(String.format(MSG_NETWORK, osNet.getName(), MSG_CREATED));
    }

    @Override
    public void updateNetwork(Network osNet) {
        checkNotNull(osNet, ERR_NULL_NETWORK);
        checkArgument(!Strings.isNullOrEmpty(osNet.getId()), ERR_NULL_NETWORK_ID);

        osNetworkStore.updateNetwork(osNet);
        log.info(String.format(MSG_NETWORK, osNet.getId(), MSG_UPDATED));
    }

    @Override
    public void removeNetwork(String netId) {
        checkArgument(!Strings.isNullOrEmpty(netId), ERR_NULL_NETWORK_ID);
        synchronized (this) {
            if (isNetworkInUse(netId)) {
                final String error = String.format(MSG_NETWORK, netId, ERR_IN_USE);
                throw new IllegalStateException(error);
            }
            Network osNet = osNetworkStore.removeNetwork(netId);
            if (osNet != null) {
                log.info(String.format(MSG_NETWORK, osNet.getName(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void createSubnet(Subnet osSubnet) {
        checkNotNull(osSubnet, ERR_NULL_SUBNET);
        checkArgument(!Strings.isNullOrEmpty(osSubnet.getId()), ERR_NULL_SUBNET_ID);
        checkArgument(!Strings.isNullOrEmpty(osSubnet.getNetworkId()), ERR_NULL_SUBNET_NET_ID);
        checkArgument(!Strings.isNullOrEmpty(osSubnet.getCidr()), ERR_NULL_SUBNET_CIDR);

        osNetworkStore.createSubnet(osSubnet);
        log.info(String.format(MSG_SUBNET, osSubnet.getCidr(), MSG_CREATED));
    }

    @Override
    public void updateSubnet(Subnet osSubnet) {
        checkNotNull(osSubnet, ERR_NULL_SUBNET);
        checkArgument(!Strings.isNullOrEmpty(osSubnet.getId()), ERR_NULL_SUBNET_ID);
        checkArgument(!Strings.isNullOrEmpty(osSubnet.getNetworkId()), ERR_NULL_SUBNET_NET_ID);
        checkArgument(!Strings.isNullOrEmpty(osSubnet.getCidr()), ERR_NULL_SUBNET_CIDR);

        osNetworkStore.updateSubnet(osSubnet);
        log.info(String.format(MSG_SUBNET, osSubnet.getCidr(), MSG_UPDATED));
    }

    @Override
    public void removeSubnet(String subnetId) {
        checkArgument(!Strings.isNullOrEmpty(subnetId), ERR_NULL_SUBNET_ID);
        synchronized (this) {
            if (isSubnetInUse(subnetId)) {
                final String error = String.format(MSG_SUBNET, subnetId, ERR_IN_USE);
                throw new IllegalStateException(error);
            }
            Subnet osSubnet = osNetworkStore.removeSubnet(subnetId);
            if (osSubnet != null) {
                log.info(String.format(MSG_SUBNET, osSubnet.getCidr(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void createPort(Port osPort) {
        checkNotNull(osPort, ERR_NULL_PORT);
        checkArgument(!Strings.isNullOrEmpty(osPort.getId()), ERR_NULL_PORT_ID);
        checkArgument(!Strings.isNullOrEmpty(osPort.getNetworkId()), ERR_NULL_PORT_NET_ID);

        osNetworkStore.createPort(osPort);
        log.info(String.format(MSG_PORT, osPort.getId(), MSG_CREATED));
    }

    @Override
    public void updatePort(Port osPort) {
        checkNotNull(osPort, ERR_NULL_PORT);
        checkArgument(!Strings.isNullOrEmpty(osPort.getId()), ERR_NULL_PORT_ID);
        checkArgument(!Strings.isNullOrEmpty(osPort.getNetworkId()), ERR_NULL_PORT_NET_ID);

        osNetworkStore.updatePort(osPort);
        log.info(String.format(MSG_PORT, osPort.getId(), MSG_UPDATED));
    }

    @Override
    public void removePort(String portId) {
        checkArgument(!Strings.isNullOrEmpty(portId), ERR_NULL_PORT_ID);
        synchronized (this) {
            if (isPortInUse(portId)) {
                final String error = String.format(MSG_PORT, portId, ERR_IN_USE);
                throw new IllegalStateException(error);
            }
            Port osPort = osNetworkStore.removePort(portId);
            if (osPort != null) {
                log.info(String.format(MSG_PORT, osPort.getId(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void clear() {
        osNetworkStore.clear();
    }

    @Override
    public Network network(String netId) {
        checkArgument(!Strings.isNullOrEmpty(netId), ERR_NULL_NETWORK_ID);
        return osNetworkStore.network(netId);
    }

    @Override
    public Set<Network> networks() {
        return osNetworkStore.networks();
    }

    @Override
    public Subnet subnet(String subnetId) {
        checkArgument(!Strings.isNullOrEmpty(subnetId), ERR_NULL_SUBNET_ID);
        return osNetworkStore.subnet(subnetId);
    }

    @Override
    public Set<Subnet> subnets() {
        return osNetworkStore.subnets();
    }

    @Override
    public Set<Subnet> subnets(String netId) {
        Set<Subnet> osSubnets = osNetworkStore.subnets().stream()
                .filter(subnet -> Objects.equals(subnet.getNetworkId(), netId))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osSubnets);
    }

    @Override
    public Port port(String portId) {
        checkArgument(!Strings.isNullOrEmpty(portId), ERR_NULL_PORT_ID);
        return osNetworkStore.port(portId);
    }

    @Override
    public Port port(org.onosproject.net.Port port) {
        String portName = port.annotations().value(PORT_NAME);
        if (Strings.isNullOrEmpty(portName)) {
            return null;
        }

        if (port.annotations().value(PORT_NAME).startsWith(PORT_NAME_PREFIX_VM)) {
            Optional<Port> osPort = osNetworkStore.ports()
                    .stream()
                    .filter(p -> p.getId().contains(portName.substring(3)))
                    .findFirst();
            return osPort.orElse(null);
        } else if (isDirectPort(portName)) {
            //Additional prefixes will be added
            Optional<Port> osPort = osNetworkStore.ports()
                    .stream()
                    .filter(p -> p.getvNicType().equals(DIRECT) && p.getProfile().get(PCISLOT) != null)
                    .filter(p -> getIntfNameFromPciAddress(p).equals(portName))
                    .findFirst();
            return osPort.orElse(null);
        } else {
            return null;
        }
    }

    private boolean isDirectPort(String portName) {
        return portNamePrefixMap().values().stream().filter(p -> portName.startsWith(p)).findAny().isPresent();
    }

    @Override
    public Set<Port> ports() {
        return ImmutableSet.copyOf(osNetworkStore.ports());
    }

    @Override
    public Set<Port> ports(String netId) {
        Set<Port> osPorts = osNetworkStore.ports().stream()
                .filter(port -> Objects.equals(port.getNetworkId(), netId))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osPorts);
    }

    @Override
    public Set<IpPrefix> getFixedIpsByNetworkType(String type) {
        if (type == null) {
            return Sets.newHashSet();
        }

        Set<Network> networks = osNetworkStore.networks();
        Set<String> networkIds = Sets.newConcurrentHashSet();

        switch (type.toUpperCase()) {
            case "FLAT" :
                networkIds = networks.stream()
                        .filter(n -> n.getNetworkType() == NetworkType.FLAT)
                        .map(IdEntity::getId).collect(Collectors.toSet());
                break;
            case "VXLAN" :
                networkIds = networks.stream()
                        .filter(n -> n.getNetworkType() == NetworkType.VXLAN)
                        .map(IdEntity::getId).collect(Collectors.toSet());
                break;
            case "VLAN" :
                networkIds = networks.stream()
                        .filter(n -> n.getNetworkType() == NetworkType.VLAN)
                        .map(IdEntity::getId).collect(Collectors.toSet());
                break;
            default:
                break;
        }

        Set<IP> ips = Sets.newConcurrentHashSet();
        for (String networkId : networkIds) {
            osNetworkStore.ports()
                    .stream()
                    .filter(p -> p.getNetworkId().equals(networkId))
                    .filter(p -> p.getFixedIps() != null)
                    .forEach(p -> ips.addAll(p.getFixedIps()));
        }

        return ips.stream().map(ip -> IpPrefix.valueOf(
                IpAddress.valueOf(ip.getIpAddress()), PREFIX_LENGTH))
                .collect(Collectors.toSet());
    }

    @Override
    public ExternalPeerRouter externalPeerRouter(IpAddress ipAddress) {
        if (externalPeerRouterMap.containsKey(ipAddress.toString())) {
            return externalPeerRouterMap.get(ipAddress.toString()).value();
        }
        return null;
    }

    @Override
    public ExternalPeerRouter externalPeerRouter(ExternalGateway externalGateway) {
        IpAddress ipAddress = getExternalPeerRouterIp(externalGateway);

        if (ipAddress == null) {
            return null;
        }

        if (externalPeerRouterMap.containsKey(ipAddress.toString())) {
            return externalPeerRouterMap.get(ipAddress.toString()).value();
        } else {
            return null;
        }
    }

    @Override
    public void deriveExternalPeerRouterMac(ExternalGateway externalGateway, Router router, VlanId vlanId) {
        log.info("deriveExternalPeerRouterMac called");

        IpAddress sourceIp = getExternalGatewaySourceIp(externalGateway, router);
        IpAddress targetIp = getExternalPeerRouterIp(externalGateway);

        if (sourceIp == null || targetIp == null) {
            log.warn("Failed to derive external router mac address because source IP {} or target IP {} is null",
                    sourceIp, targetIp);
            return;
        }

        if (externalPeerRouterMap.containsKey(targetIp.toString()) &&
                !externalPeerRouterMap.get(
                        targetIp.toString()).value().externalPeerRouterMac().equals(MacAddress.NONE)) {
            return;
        }

        MacAddress sourceMac = Constants.DEFAULT_GATEWAY_MAC;
        Ethernet ethRequest = ARP.buildArpRequest(sourceMac.toBytes(),
                sourceIp.toOctets(),
                targetIp.toOctets(),
                vlanId.id());

        if (osNodeService.completeNodes(OpenstackNode.NodeType.GATEWAY).isEmpty()) {
            log.warn("There's no complete gateway");
            return;
        }
        OpenstackNode gatewayNode = osNodeService.completeNodes(OpenstackNode.NodeType.GATEWAY)
                .stream()
                .findFirst()
                .orElse(null);

        if (gatewayNode == null) {
            return;
        }

        if (gatewayNode.uplinkPortNum() == null) {
            log.warn("There's no uplink port for gateway node {}", gatewayNode.toString());
            return;
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(gatewayNode.uplinkPortNum())
                .build();

        packetService.emit(new DefaultOutboundPacket(
                gatewayNode.intgBridge(),
                treatment,
                ByteBuffer.wrap(ethRequest.serialize())));

        externalPeerRouterMap.put(
                targetIp.toString(), new DefaultExternalPeerRouter(targetIp, MacAddress.NONE, vlanId));

        log.info("Initializes external peer router map with peer router IP {}", targetIp.toString());
    }

    @Override
    public void deleteExternalPeerRouter(ExternalGateway externalGateway) {
        if (externalGateway == null) {
            return;
        }

        IpAddress targetIp = getExternalPeerRouterIp(externalGateway);
        if (targetIp == null) {
            return;
        }

        if (externalPeerRouterMap.containsKey(targetIp.toString())) {
            externalPeerRouterMap.remove(targetIp.toString());
        }
    }

    @Override
    public void deleteExternalPeerRouter(String ipAddress) {
        if (ipAddress == null) {
            return;
        }

        if (externalPeerRouterMap.containsKey(ipAddress)) {
            externalPeerRouterMap.remove(ipAddress);
        }

    }

    @Override
    public void updateExternalPeerRouterMac(IpAddress ipAddress, MacAddress macAddress) {
        try {
            externalPeerRouterMap.computeIfPresent(ipAddress.toString(), (id, existing) ->
                new DefaultExternalPeerRouter(ipAddress, macAddress, existing.externalPeerRouterVlanId()));

            log.info("Updated external peer router map {}",
                    externalPeerRouterMap.get(ipAddress.toString()).value().toString());
        } catch (Exception e) {
            log.error("Exception occurred because of {}", e.toString());
        }
    }


    @Override
    public void updateExternalPeerRouter(IpAddress ipAddress, MacAddress macAddress, VlanId vlanId) {
        try {
            externalPeerRouterMap.computeIfPresent(ipAddress.toString(), (id, existing) ->
                new DefaultExternalPeerRouter(ipAddress, macAddress, vlanId));
        } catch (Exception e) {
            log.error("Exception occurred because of {}", e.toString());
        }
    }

    @Override
    public MacAddress externalPeerRouterMac(ExternalGateway externalGateway) {
        IpAddress ipAddress = getExternalPeerRouterIp(externalGateway);

        if (ipAddress == null) {
            return null;
        }
        if (externalPeerRouterMap.containsKey(ipAddress.toString())) {
            return externalPeerRouterMap.get(ipAddress.toString()).value().externalPeerRouterMac();
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void updateExternalPeerRouterVlan(IpAddress ipAddress, VlanId vlanId) {

        try {
            externalPeerRouterMap.computeIfPresent(ipAddress.toString(), (id, existing) ->
                    new DefaultExternalPeerRouter(ipAddress, existing.externalPeerRouterMac(), vlanId));

        } catch (Exception e) {
            log.error("Exception occurred because of {}", e.toString());
        }
    }

    @Override
    public Set<ExternalPeerRouter> externalPeerRouters() {
        return ImmutableSet.copyOf(externalPeerRouterMap.asJavaMap().values());
    }
    private boolean isNetworkInUse(String netId) {
        return !subnets(netId).isEmpty() && !ports(netId).isEmpty();
    }

    private boolean isSubnetInUse(String subnetId) {
        // TODO add something if needed
        return false;
    }

    private boolean isPortInUse(String portId) {
        // TODO add something if needed
        return false;
    }

    private class InternalNetworkStoreDelegate implements OpenstackNetworkStoreDelegate {

        @Override
        public void notify(OpenstackNetworkEvent event) {
            if (event != null) {
                log.trace("send openstack switching event {}", event);
                process(event);
            }
        }
    }

    private IpAddress getExternalGatewaySourceIp(ExternalGateway externalGateway, Router router) {
        Port exGatewayPort = ports(externalGateway.getNetworkId())
                .stream()
                .filter(port -> Objects.equals(port.getDeviceId(), router.getId()))
                .findAny().orElse(null);
        if (exGatewayPort == null) {
            log.warn("no external gateway port for router({})", router.getName());
            return null;
        }

        IP ipAddress = exGatewayPort.getFixedIps().stream().findFirst().orElse(null);

        return ipAddress == null ? null : IpAddress.valueOf(ipAddress.getIpAddress());
    }

    private IpAddress getExternalPeerRouterIp(ExternalGateway externalGateway) {
        if (externalGateway == null) {
            return null;
        }
        Optional<Subnet> externalSubnet = subnets(externalGateway.getNetworkId())
                .stream()
                .findFirst();

        return externalSubnet.map(subnet -> IpAddress.valueOf(subnet.getGateway())).orElse(null);
    }
}
