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
import org.onosproject.openstacknetworking.api.OpenstackNetwork;
import org.onosproject.openstacknetworking.api.OpenstackNetwork.Type;
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
import org.onosproject.store.service.Versioned;
import org.openstack4j.model.network.ExternalGateway;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknetworking.api.Constants.DIRECT;
import static org.onosproject.openstacknetworking.api.Constants.PCISLOT;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.FLAT;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.GENEVE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.GRE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.LOCAL;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.VLAN;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.VXLAN;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.deriveResourceName;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getIntfNameFromPciAddress;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.vnicType;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfacing OpenStack network,
 * subnet, and port.
 */

@Component(
    immediate = true,
    service = { OpenstackNetworkAdminService.class, OpenstackNetworkService.class }
)
public class OpenstackNetworkManager
        extends ListenerRegistry<OpenstackNetworkEvent, OpenstackNetworkListener>
        implements OpenstackNetworkAdminService, OpenstackNetworkService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_NETWORK  = "OpenStack network %s %s";
    private static final String MSG_NETWORK_TYPE  = "OpenStack network type %s %s";
    private static final String MSG_SUBNET  = "OpenStack subnet %s %s";
    private static final String MSG_PORT = "OpenStack port %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NOT_FOUND = " does not exist";
    private static final String ERR_DUPLICATE = " already exists";

    private static final String ERR_NULL_NETWORK  =
                                "OpenStack network cannot be null";
    private static final String ERR_NULL_NETWORK_ID  =
                                "OpenStack network ID cannot be null";
    private static final String ERR_NULL_SUBNET =
                                "OpenStack subnet cannot be null";
    private static final String ERR_NULL_SUBNET_ID =
                                "OpenStack subnet ID cannot be null";
    private static final String ERR_NULL_SUBNET_NET_ID =
                                "OpenStack subnet network ID cannot be null";
    private static final String ERR_NULL_SUBNET_CIDR =
                                "OpenStack subnet CIDR cannot be null";
    private static final String ERR_NULL_PORT =
                                "OpenStack port cannot be null";
    private static final String ERR_NULL_PORT_ID =
                                "OpenStack port ID cannot be null";
    private static final String ERR_NULL_PORT_NET_ID =
                                "OpenStack port network ID cannot be null";
    private static final String ERR_NULL_PEER_ROUTER =
                                "External peer router cannot be null";
    private static final String ERR_NULL_PEER_ROUTER_IP =
                                "External peer router IP cannot be null";
    private static final String ERR_NULL_PEER_ROUTER_MAC =
                                "External peer router MAC cannot be null";

    private static final String ERR_IN_USE = " still in use";

    private static final int PREFIX_LENGTH = 32;


    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNetworkStore osNetworkStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;

    private final OpenstackNetworkStoreDelegate
                                delegate = new InternalNetworkStoreDelegate();

    private ConsistentMap<String, OpenstackNetwork> augmentedNetworkMap;

    private static final KryoNamespace
            SERIALIZER_AUGMENTED_NETWORK_MAP = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(OpenstackNetwork.Type.class)
            .register(OpenstackNetwork.class)
            .register(DefaultOpenstackNetwork.class)
            .build();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(Constants.OPENSTACK_NETWORKING_APP_ID);

        osNetworkStore.setDelegate(delegate);
        log.info("Started");

        augmentedNetworkMap = storageService.<String, OpenstackNetwork>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_AUGMENTED_NETWORK_MAP))
                .withName("augmented-networkmap")
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

        OpenstackNetwork finalAugmentedNetwork = buildAugmentedNetworkFromType(osNet);
        augmentedNetworkMap.compute(osNet.getId(), (id, existing) -> {
            final String error = osNet.getId() + ERR_DUPLICATE;
            checkArgument(existing == null, error);
            return finalAugmentedNetwork;
        });

        log.info(String.format(MSG_NETWORK, deriveResourceName(osNet), MSG_CREATED));
    }

    @Override
    public void updateNetwork(Network osNet) {
        checkNotNull(osNet, ERR_NULL_NETWORK);
        checkArgument(!Strings.isNullOrEmpty(osNet.getId()), ERR_NULL_NETWORK_ID);

        osNetworkStore.updateNetwork(osNet);

        OpenstackNetwork finalAugmentedNetwork = buildAugmentedNetworkFromType(osNet);
        augmentedNetworkMap.compute(osNet.getId(), (id, existing) -> {
            final String error = osNet.getId() + ERR_NOT_FOUND;
            checkArgument(existing != null, error);
            return finalAugmentedNetwork;
        });

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
                log.info(String.format(MSG_NETWORK, deriveResourceName(osNet), MSG_REMOVED));
            }

            Versioned<OpenstackNetwork> augmentedNetwork = augmentedNetworkMap.remove(netId);
            if (augmentedNetwork != null) {
                log.info(String.format(MSG_NETWORK_TYPE,
                                    augmentedNetwork.value().type(), MSG_REMOVED));
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
        augmentedNetworkMap.clear();
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

        try {
            Optional<Port> osPort;
            switch (vnicType(portName)) {
                case NORMAL:
                    osPort = osNetworkStore.ports()
                            .stream()
                            .filter(p -> p.getId().contains(portName.substring(3)))
                            .findFirst();
                    return osPort.orElse(null);
                case DIRECT:
                    //Additional prefixes will be added
                    osPort = osNetworkStore.ports()
                            .stream()
                            .filter(p -> p.getvNicType().equals(DIRECT) &&
                                            p.getProfile().get(PCISLOT) != null)
                            .filter(p -> requireNonNull(
                                    getIntfNameFromPciAddress(p)).equals(portName))
                            .findFirst();
                    return osPort.orElse(null);
                default:
                    return null;
            }
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException occurred because of {}", e);
            return null;
        }
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

        Set<String> networkIds = Sets.newConcurrentHashSet();

        switch (type.toUpperCase()) {
            case Constants.FLAT :
                networkIds = augmentedNetworkMap.asJavaMap().entrySet().stream()
                        .filter(e -> e.getValue().type() == FLAT)
                        .map(Map.Entry::getKey).collect(Collectors.toSet());
                break;
            case Constants.VXLAN :
                networkIds = augmentedNetworkMap.asJavaMap().entrySet().stream()
                        .filter(e -> e.getValue().type() == VXLAN)
                        .map(Map.Entry::getKey).collect(Collectors.toSet());
                break;
            case Constants.GRE :
                networkIds = augmentedNetworkMap.asJavaMap().entrySet().stream()
                        .filter(e -> e.getValue().type() == GRE)
                        .map(Map.Entry::getKey).collect(Collectors.toSet());
                break;
            case Constants.VLAN :
                networkIds = augmentedNetworkMap.asJavaMap().entrySet().stream()
                        .filter(e -> e.getValue().type() == VLAN)
                        .map(Map.Entry::getKey).collect(Collectors.toSet());
                break;
            case Constants.GENEVE :
                networkIds = augmentedNetworkMap.asJavaMap().entrySet().stream()
                        .filter(e -> e.getValue().type() == GENEVE)
                        .map(Map.Entry::getKey).collect(Collectors.toSet());
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
        return osNetworkStore.externalPeerRouter(ipAddress.toString());
    }

    @Override
    public ExternalPeerRouter externalPeerRouter(ExternalGateway externalGateway) {
        IpAddress ipAddress = getExternalPeerRouterIp(externalGateway);

        if (ipAddress == null) {
            return null;
        }

        return externalPeerRouter(ipAddress);
    }

    @Override
    public void deriveExternalPeerRouterMac(ExternalGateway externalGateway,
                                            Router router, VlanId vlanId) {
        log.info("deriveExternalPeerRouterMac called");

        IpAddress sourceIp = getExternalGatewaySourceIp(externalGateway, router);
        IpAddress targetIp = getExternalPeerRouterIp(externalGateway);

        if (sourceIp == null || targetIp == null) {
            log.warn("Failed to derive external router mac address because " +
                            "source IP {} or target IP {} is null", sourceIp, targetIp);
            return;
        }

        ExternalPeerRouter peerRouter = osNetworkStore.externalPeerRouter(targetIp.toString());

        // if peer router's MAC address is not NONE, we assume that peer router's
        // MAC address has been derived
        if (peerRouter != null && !peerRouter.macAddress().equals(MacAddress.NONE)) {
            return;
        }

        MacAddress sourceMac = Constants.DEFAULT_GATEWAY_MAC;
        Ethernet ethRequest = ARP.buildArpRequest(sourceMac.toBytes(),
                sourceIp.toOctets(),
                targetIp.toOctets(),
                vlanId.id());

        if (osNodeService.completeNodes(GATEWAY).isEmpty()) {
            log.warn("There's no complete gateway");
            return;
        }
        OpenstackNode gatewayNode = osNodeService.completeNodes(GATEWAY)
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

        ExternalPeerRouter derivedRouter = DefaultExternalPeerRouter.builder()
                .ipAddress(targetIp)
                .macAddress(MacAddress.NONE)
                .vlanId(vlanId)
                .build();
        osNetworkStore.createExternalPeerRouter(derivedRouter);
        log.info("Initializes external peer router map with peer router IP {}",
                                                            targetIp.toString());
    }

    @Override
    public void deleteExternalPeerRouter(ExternalGateway externalGateway) {
        if (externalGateway == null) {
            return;
        }

        IpAddress targetIp = getExternalPeerRouterIp(externalGateway);
        deleteExternalPeerRouter(targetIp.toString());
    }

    @Override
    public void deleteExternalPeerRouter(String ipAddress) {
        osNetworkStore.removeExternalPeerRouter(ipAddress);
    }

    @Override
    public void updateExternalPeerRouterMac(IpAddress ipAddress,
                                            MacAddress macAddress) {
        updateExternalPeerRouter(ipAddress, macAddress, null);
    }

    @Override
    public void updateExternalPeerRouter(IpAddress ipAddress,
                                         MacAddress macAddress,
                                         VlanId vlanId) {
        checkNotNull(ipAddress, ERR_NULL_PEER_ROUTER_IP);

        ExternalPeerRouter existingPeerRouter =
                osNetworkStore.externalPeerRouter(ipAddress.toString());

        if (existingPeerRouter != null) {
            ExternalPeerRouter.Builder urBuilder = DefaultExternalPeerRouter.builder()
                    .ipAddress(ipAddress);

            if (macAddress == null) {
                urBuilder.macAddress(existingPeerRouter.macAddress());
            } else {
                urBuilder.macAddress(macAddress);
            }

            if (vlanId == null) {
                urBuilder.vlanId(existingPeerRouter.vlanId());
            } else {
                urBuilder.vlanId(vlanId);
            }
            osNetworkStore.updateExternalPeerRouter(urBuilder.build());
        }
    }

    @Override
    public MacAddress externalPeerRouterMac(ExternalGateway externalGateway) {
        IpAddress ipAddress = getExternalPeerRouterIp(externalGateway);

        if (ipAddress == null) {
            return null;
        }

        ExternalPeerRouter peerRouter =
                osNetworkStore.externalPeerRouter(ipAddress.toString());

        if (peerRouter == null) {
            throw new NoSuchElementException();
        } else {
            return peerRouter.macAddress();
        }
    }

    @Override
    public void updateExternalPeerRouterVlan(IpAddress ipAddress, VlanId vlanId) {
        updateExternalPeerRouter(ipAddress, null, vlanId);
    }

    @Override
    public Set<ExternalPeerRouter> externalPeerRouters() {
        return ImmutableSet.copyOf(osNetworkStore.externalPeerRouters());
    }

    @Override
    public IpPrefix ipPrefix(String portId) {
        checkNotNull(portId);

        Port port = port(portId);

        checkNotNull(port);

        IpAddress ipAddress = port.getFixedIps().stream()
                .map(ip -> IpAddress.valueOf(ip.getIpAddress()))
                .findAny().orElse(null);

        checkNotNull(ipAddress);

        Network network = network(port.getNetworkId());

        checkNotNull(network);

        return subnets(network.getId()).stream()
                .map(s -> IpPrefix.valueOf(s.getCidr()))
                .filter(prefix -> prefix.contains(ipAddress))
                .findAny().orElse(null);
    }

    @Override
    public Type networkType(String netId) {
        OpenstackNetwork network = augmentedNetworkMap.asJavaMap().get(netId);

        checkNotNull(network);

        return network.type();
    }

    @Override
    public String gatewayIp(String portId) {
        checkNotNull(portId);

        Port port = port(portId);

        checkNotNull(port);

        IpAddress ipAddress = port.getFixedIps().stream()
                .map(ip -> IpAddress.valueOf(ip.getIpAddress()))
                .findAny().orElse(null);

        checkNotNull(ipAddress);

        Network network = network(port.getNetworkId());

        checkNotNull(network);

        return subnets(network.getId()).stream()
                .filter(s -> IpPrefix.valueOf(s.getCidr()).contains(ipAddress))
                .map(Subnet::getGateway)
                .findAny().orElse(null);
    }

    @Override
    public String segmentId(String netId) {
        Network network = network(netId);

        checkNotNull(network);

        return network.getProviderSegID();
    }

    private OpenstackNetwork buildAugmentedNetworkFromType(Network osNet) {
        OpenstackNetwork augmentedNetwork = null;
        if (osNet.getNetworkType() == null) {
            augmentedNetwork = new DefaultOpenstackNetwork(osNet.getId(), GENEVE);
        } else {
            switch (osNet.getNetworkType()) {
                case FLAT:
                    augmentedNetwork = new DefaultOpenstackNetwork(osNet.getId(), FLAT);
                    break;
                case VLAN:
                    augmentedNetwork = new DefaultOpenstackNetwork(osNet.getId(), VLAN);
                    break;
                case VXLAN:
                    augmentedNetwork = new DefaultOpenstackNetwork(osNet.getId(), VXLAN);
                    break;
                case GRE:
                    augmentedNetwork = new DefaultOpenstackNetwork(osNet.getId(), GRE);
                    break;
                case LOCAL:
                    augmentedNetwork = new DefaultOpenstackNetwork(osNet.getId(), LOCAL);
                    break;
                default:
                    break;
            }
        }

        return augmentedNetwork;
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

    private IpAddress getExternalGatewaySourceIp(ExternalGateway externalGateway,
                                                 Router router) {
        Port exGatewayPort = ports(externalGateway.getNetworkId())
                .stream()
                .filter(port -> Objects.equals(port.getDeviceId(), router.getId()))
                .findAny().orElse(null);
        if (exGatewayPort == null) {
            log.warn("no external gateway port for router({})", deriveResourceName(router));
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

        return externalSubnet.map(subnet ->
                    IpAddress.valueOf(subnet.getGateway())).orElse(null);
    }
}