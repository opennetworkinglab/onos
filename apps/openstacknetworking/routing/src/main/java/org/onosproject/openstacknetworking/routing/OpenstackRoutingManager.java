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
package org.onosproject.openstacknetworking.routing;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.UDP;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostDescription;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openstackinterface.OpenstackFloatingIP;
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.openstackinterface.OpenstackRouter;
import org.onosproject.openstackinterface.OpenstackRouterInterface;
import org.onosproject.openstacknetworking.OpenstackRoutingService;
import org.onosproject.scalablegateway.api.ScalableGatewayService;
import org.onosproject.openstacknetworking.routing.OpenstackFloatingIPHandler.Action;
import org.onosproject.openstacknetworking.Constants;
import org.onosproject.openstacknode.OpenstackNode;
import org.onosproject.openstacknode.OpenstackNodeEvent;
import org.onosproject.openstacknode.OpenstackNodeListener;
import org.onosproject.openstacknode.OpenstackNodeService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;

@Component(immediate = true)
@Service
/**
 * Populates flow rules about L3 functionality for VMs in Openstack.
 */
public class OpenstackRoutingManager implements OpenstackRoutingService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackInterfaceService openstackService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry hostProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ScalableGatewayService gatewayService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService nodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    private ApplicationId appId;
    private ConsistentMap<Integer, String> tpPortNumMap; // Map<PortNum, allocated VM`s Mac & destionation Ip address>
    private ConsistentMap<String, OpenstackFloatingIP> floatingIpMap; // Map<FloatingIp`s Id, FloatingIp object>
    // Map<RouterInterface`s portId, Corresponded port`s network id>
    private ConsistentMap<String, String> routerInterfaceMap;
    private static final ProviderId PID = new ProviderId("of", "org.onosproject.openstackroutering", true);
    private static final String APP_ID = "org.onosproject.openstackrouting";
    private static final String DEVICE_OWNER_ROUTER_INTERFACE = "network:router_interface";
    private static final String FLOATING_IP_MAP_NAME = "openstackrouting-floatingip";
    private static final String TP_PORT_MAP_NAME = "openstackrouting-tpportnum";
    private static final String ROUTER_INTERFACE_MAP_NAME = "openstackrouting-routerinterface";
    private static final String COLON = ":";
    private static final int PNAT_PORT_EXPIRE_TIME = 1200 * 1000;
    private static final int TP_PORT_MINIMUM_NUM = 1024;
    private static final int TP_PORT_MAXIMUM_NUM = 65535;

    private static final KryoNamespace.Builder FLOATING_IP_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(OpenstackFloatingIP.FloatingIpStatus.class)
            .register(OpenstackFloatingIP.class);

    private static final KryoNamespace.Builder NUMBER_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API);

    private static final KryoNamespace.Builder ROUTER_INTERFACE_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API);

    private InternalPacketProcessor internalPacketProcessor = new InternalPacketProcessor();
    private InternalHostListener internalHostListener = new InternalHostListener();
    private InternalOpenstackNodeListener internalNodeListener = new InternalOpenstackNodeListener();
    private ExecutorService l3EventExecutorService =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackrouting", "L3-event"));
    private ExecutorService icmpEventExecutorService =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackrouting", "icmp-event"));
    private ExecutorService arpEventExecutorService =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackrouting", "arp-event"));
    private OpenstackIcmpHandler openstackIcmpHandler;
    private OpenstackRoutingArpHandler openstackArpHandler;
    private OpenstackRoutingRulePopulator rulePopulator;

    private HostProviderService hostProviderService;
    private final HostProvider hostProvider = new InternalHostProvider();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_ID);
        hostService.addListener(internalHostListener);
        nodeService.addListener(internalNodeListener);
        hostProviderService = hostProviderRegistry.register(hostProvider);

        floatingIpMap = storageService.<String, OpenstackFloatingIP>consistentMapBuilder()
                .withSerializer(Serializer.using(FLOATING_IP_SERIALIZER.build()))
                .withName(FLOATING_IP_MAP_NAME)
                .withApplicationId(appId)
                .build();
        tpPortNumMap = storageService.<Integer, String>consistentMapBuilder()
                .withSerializer(Serializer.using(NUMBER_SERIALIZER.build()))
                .withName(TP_PORT_MAP_NAME)
                .withApplicationId(appId)
                .build();
        routerInterfaceMap = storageService.<String, String>consistentMapBuilder()
                .withSerializer(Serializer.using(ROUTER_INTERFACE_SERIALIZER.build()))
                .withName(ROUTER_INTERFACE_MAP_NAME)
                .withApplicationId(appId)
                .build();

        log.info("started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(internalPacketProcessor);
        hostService.removeListener(internalHostListener);
        nodeService.removeListener(internalNodeListener);

        l3EventExecutorService.shutdown();
        icmpEventExecutorService.shutdown();
        arpEventExecutorService.shutdown();

        floatingIpMap.clear();
        tpPortNumMap.clear();
        routerInterfaceMap.clear();

        log.info("stopped");
    }


    @Override
    public void createFloatingIP(OpenstackFloatingIP openstackFloatingIp) {
        floatingIpMap.put(openstackFloatingIp.id(), openstackFloatingIp);
    }

    @Override
    public void updateFloatingIP(OpenstackFloatingIP openstackFloatingIp) {
        if (!floatingIpMap.containsKey(openstackFloatingIp.id())) {
            log.warn("There`s no information about {} in FloatingIpMap", openstackFloatingIp.id());
            return;
        }
        if (openstackFloatingIp.portId() == null || openstackFloatingIp.portId().equals("null")) {
            OpenstackFloatingIP floatingIp = floatingIpMap.get(openstackFloatingIp.id()).value();
            // XXX When the VM has been removed, host information has been removed or not ???
            Optional<Host> host = hostService.getHostsByIp(openstackFloatingIp.fixedIpAddress().getIp4Address())
                    .stream()
                    .findFirst();
            if (!host.isPresent()) {
                log.warn("No Host info with the VM IP the Floating IP address {} is found",
                        openstackFloatingIp.floatingIpAddress());
                return;
            }
            l3EventExecutorService.execute(
                    new OpenstackFloatingIPHandler(rulePopulator, floatingIp, Action.DISSASSOCIATE, host.get()));
            floatingIpMap.replace(floatingIp.id(), openstackFloatingIp);
            registerFloatingIpToHostService(openstackFloatingIp, Action.DISSASSOCIATE);
        } else {
            floatingIpMap.put(openstackFloatingIp.id(), openstackFloatingIp);
            l3EventExecutorService.execute(
                    new OpenstackFloatingIPHandler(rulePopulator, openstackFloatingIp, Action.ASSOCIATE, null));
            registerFloatingIpToHostService(openstackFloatingIp, Action.ASSOCIATE);
        }
    }

    @Override
    public void deleteFloatingIP(String id) {
        floatingIpMap.remove(id);
    }

    @Override
    public void createRouter(OpenstackRouter openstackRouter) {
    }

    @Override
    public void updateRouter(OpenstackRouter openstackRouter) {
        if (openstackRouter.gatewayExternalInfo().externalFixedIps().size() > 0) {
            checkExternalConnection(openstackRouter, getOpenstackRouterInterface(openstackRouter));
        } else {
            unsetExternalConnection();
        }
    }

    private void unsetExternalConnection() {
        Collection<OpenstackRouter> internalRouters = getExternalRouter(false);
        internalRouters.forEach(r ->
                getOpenstackRouterInterface(r).forEach(i -> rulePopulator.removeExternalRules(i)));
    }

    private Collection<OpenstackRouter> getExternalRouter(boolean externalConnection) {
        List<OpenstackRouter> routers;
        if (externalConnection) {
            routers = openstackService.routers()
                    .stream()
                    .filter(r -> (r.gatewayExternalInfo().externalFixedIps().size() > 0))
                    .collect(Collectors.toList());
        } else {
            routers = openstackService.routers()
                    .stream()
                    .filter(r -> (r.gatewayExternalInfo().externalFixedIps().size() == 0))
                    .collect(Collectors.toList());
        }
        return routers;
    }

    @Override
    public void deleteRouter(String id) {
        //TODO : In now, there`s nothing to do for deleteRouter process. It is reserved.
    }

    @Override
    public void updateRouterInterface(OpenstackRouterInterface routerInterface) {
        List<OpenstackRouterInterface> routerInterfaces = Lists.newArrayList();
        routerInterfaces.add(routerInterface);
        checkExternalConnection(getOpenstackRouter(routerInterface.id()), routerInterfaces);
        setL3Connection(getOpenstackRouter(routerInterface.id()), null);
        routerInterfaceMap.put(routerInterface.portId(), openstackService.port(routerInterface.portId()).networkId());
    }

    /**
     * Set flow rules for traffic between two different subnets when more than one subnets
     * connected to a router.
     *
     * @param openstackRouter OpenstackRouter Info
     * @param openstackPort OpenstackPort Info
     */
    private void setL3Connection(OpenstackRouter openstackRouter, OpenstackPort openstackPort) {
        Collection<OpenstackRouterInterface> interfaceList = getOpenstackRouterInterface(openstackRouter);

        if (interfaceList.size() < 2) {
            return;
        }
        if (openstackPort == null) {
            interfaceList.forEach(i -> {
                OpenstackPort interfacePort = openstackService.port(i.portId());
                openstackService.ports()
                        .stream()
                        .filter(p -> p.networkId().equals(interfacePort.networkId())
                                && !p.deviceOwner().equals(DEVICE_OWNER_ROUTER_INTERFACE))
                        .forEach(p -> rulePopulator.populateL3Rules(p,
                                    getL3ConnectionList(p.networkId(), interfaceList)));

            });
        } else {
            rulePopulator.populateL3Rules(openstackPort, getL3ConnectionList(openstackPort.networkId(), interfaceList));
        }

    }

    private List<OpenstackRouterInterface> getL3ConnectionList(String networkId,
                                                               Collection<OpenstackRouterInterface> interfaceList) {
        List<OpenstackRouterInterface> targetList = Lists.newArrayList();
        interfaceList.forEach(i -> {
            OpenstackPort port = openstackService.port(i.portId());
            if (!port.networkId().equals(networkId)) {
                targetList.add(i);
            }
        });
        return targetList;
    }

    @Override
    public void removeRouterInterface(OpenstackRouterInterface routerInterface) {
        OpenstackRouter router = openstackService.router(routerInterface.id());
        Collection<OpenstackRouterInterface> interfaceList = getOpenstackRouterInterface(router);
        if (interfaceList.size() == 1) {
            List<OpenstackRouterInterface> newList = Lists.newArrayList();
            newList.add(routerInterface);
            interfaceList.forEach(i -> removeL3RulesForRouterInterface(i, router, newList));
        }
        removeL3RulesForRouterInterface(routerInterface, router, null);
        rulePopulator.removeExternalRules(routerInterface);
        routerInterfaceMap.remove(routerInterface.portId());
    }

    private void removeL3RulesForRouterInterface(OpenstackRouterInterface routerInterface, OpenstackRouter router,
                                                 List<OpenstackRouterInterface> newList) {
        if (!routerInterfaceMap.containsKey(routerInterface.portId())) {
            log.warn("No router interface information found for {}", routerInterface.portId());
            return;
        }
        openstackService.ports(routerInterfaceMap.get(routerInterface.portId()).value()).forEach(p -> {
                    Ip4Address vmIp = (Ip4Address) p.fixedIps().values().toArray()[0];
                    if (newList == null) {
                        rulePopulator.removeL3Rules(vmIp,
                                getL3ConnectionList(p.networkId(), getOpenstackRouterInterface(router)));
                    } else {
                        rulePopulator.removeL3Rules(vmIp, newList);
                    }
                }
        );
    }

    @Override
    public String networkIdForRouterInterface(String portId) {
        return routerInterfaceMap.get(portId).value();
    }

    private Collection<OpenstackFloatingIP> associatedFloatingIps() {
        List<OpenstackFloatingIP> fIps = Lists.newArrayList();
        floatingIpMap.values()
                .stream()
                .filter(fIp -> fIp.value().portId() != null)
                .forEach(fIp -> fIps.add(fIp.value()));
        return fIps;
    }

    private void reloadInitL3Rules() {

        l3EventExecutorService.execute(() ->
                openstackService.ports()
                        .stream()
                        .forEach(p ->
                        {
                            if (p.deviceOwner().equals(DEVICE_OWNER_ROUTER_INTERFACE)) {
                                updateRouterInterface(portToRouterInterface(p));
                            } else {
                                Optional<Ip4Address> vmIp = p.fixedIps().values().stream().findAny();
                                if (vmIp.isPresent()) {
                                    OpenstackFloatingIP floatingIP = getOpenstackFloatingIp(vmIp.get());
                                    if (floatingIP != null) {
                                        updateFloatingIP(floatingIP);
                                    }
                                }
                            }
                        })
        );
    }

    private OpenstackRouterInterface portToRouterInterface(OpenstackPort p) {
        OpenstackRouterInterface.Builder osBuilder = new OpenstackRouterInterface.Builder()
                .id(checkNotNull(p.deviceId()))
                .tenantId(checkNotNull(openstackService.network(p.networkId()).tenantId()))
                .subnetId(checkNotNull(p.fixedIps().keySet().stream().findFirst().orElse(null)).toString())
                .portId(checkNotNull(p.id()));

        return osBuilder.build();
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            DeviceId senderDeviceId = context.inPacket().receivedFrom().deviceId();
            if (!nodeService.routerBridge(senderDeviceId).isPresent()) {
                log.warn("No router bridge for {} is found.", senderDeviceId);
                return;
            }
            if (context.isHandled()) {
                return;
            } else if (!checkGatewayNode(context.inPacket().receivedFrom().deviceId())) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethernet = pkt.parsed();

            //TODO: Considers IPv6 later.
            if (ethernet == null) {
                return;
            } else if (ethernet.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 iPacket = (IPv4) ethernet.getPayload();
                switch (iPacket.getProtocol()) {
                    case IPv4.PROTOCOL_ICMP:

                        icmpEventExecutorService.execute(() ->
                                openstackIcmpHandler.processIcmpPacket(context, ethernet));
                        break;
                    case IPv4.PROTOCOL_UDP:
                        // don't process DHCP
                        UDP udpPacket = (UDP) iPacket.getPayload();
                        if (udpPacket.getDestinationPort() == UDP.DHCP_SERVER_PORT &&
                                udpPacket.getSourcePort() == UDP.DHCP_CLIENT_PORT) {
                            break;
                        }
                    default:
                        int portNum = getPortNum(ethernet.getSourceMAC(), iPacket.getDestinationAddress());
                        DeviceId deviceId = pkt.receivedFrom().deviceId();
                        Port port = null;
                        port = deviceService.getPort(deviceId,
                                gatewayService.getGatewayExternalPort(deviceId));
                        if (port != null) {
                            OpenstackPort openstackPort = getOpenstackPort(ethernet.getSourceMAC(),
                                    Ip4Address.valueOf(iPacket.getSourceAddress()));
                            l3EventExecutorService.execute(new OpenstackPnatHandler(rulePopulator, context,
                                    portNum, openstackPort, port));

                        } else {
                            log.warn("There`s no external interface");
                        }

                        break;
                }
            } else if (ethernet.getEtherType() == Ethernet.TYPE_ARP) {
                arpEventExecutorService.execute(() ->
                        openstackArpHandler.processArpPacketFromRouter(context, ethernet));
            }
        }

        private int getPortNum(MacAddress sourceMac, int destinationAddress) {
            int portNum = findUnusedPortNum();
            if (portNum == 0) {
                clearPortNumMap();
                portNum = findUnusedPortNum();
            }
            tpPortNumMap.put(portNum, sourceMac.toString().concat(COLON).concat(String.valueOf(destinationAddress)));
            return portNum;
        }

        private int findUnusedPortNum() {
            for (int i = TP_PORT_MINIMUM_NUM; i < TP_PORT_MAXIMUM_NUM; i++) {
                if (!tpPortNumMap.containsKey(i)) {
                    return i;
                }
            }
            return 0;
        }

    }

    private boolean checkGatewayNode(DeviceId deviceId) {
        return gatewayService.getGatewayDeviceIds().contains(deviceId);
    }

    private void clearPortNumMap() {
        tpPortNumMap.entrySet().forEach(e -> {
            if (System.currentTimeMillis() - e.getValue().creationTime() > PNAT_PORT_EXPIRE_TIME) {
                tpPortNumMap.remove(e.getKey());
            }
        });
    }

    private Optional<Port> getExternalPort(DeviceId deviceId, String interfaceName) {
        return deviceService.getPorts(deviceId)
                .stream()
                .filter(p -> p.annotations().value(PORT_NAME).equals(interfaceName))
                .findAny();
    }

    private void checkExternalConnection(OpenstackRouter router,
                                         Collection<OpenstackRouterInterface> interfaces) {
        checkNotNull(router, "Router can not be null");
        checkNotNull(interfaces, "routerInterfaces can not be null");
        Ip4Address externalIp = router.gatewayExternalInfo().externalFixedIps()
                .values().stream().findFirst().orElse(null);
        if ((externalIp == null) || (!router.gatewayExternalInfo().isEnablePnat())) {
            log.debug("Not satisfied to set pnat configuration");
            return;
        }
        interfaces.forEach(this::initiateL3Rule);
    }

    private Optional<OpenstackRouter> getRouterfromExternalIp(Ip4Address externalIp) {
        return getExternalRouter(true)
                .stream()
                .filter(r -> r.gatewayExternalInfo()
                        .externalFixedIps()
                        .values()
                        .stream()
                        .findAny()
                        .get()
                        .equals(externalIp))
                .findAny();
    }

    private void initiateL3Rule(OpenstackRouterInterface routerInterface) {
        long vni = Long.parseLong(openstackService.network(openstackService
                .port(routerInterface.portId()).networkId()).segmentId());
        rulePopulator.populateExternalRules(vni);
    }

    private Collection<OpenstackRouterInterface> getOpenstackRouterInterface(OpenstackRouter router) {
        List<OpenstackRouterInterface> interfaces = Lists.newArrayList();
        openstackService.ports()
                .stream()
                .filter(p -> p.deviceOwner().equals(DEVICE_OWNER_ROUTER_INTERFACE)
                        && p.deviceId().equals(router.id()))
                .forEach(p -> interfaces.add(portToRouterInterface(p)));
        return interfaces;
    }

    private OpenstackRouter getOpenstackRouter(String id) {
        return openstackService.routers().stream().filter(r ->
                r.id().equals(id)).iterator().next();
    }

    private OpenstackPort getOpenstackPort(MacAddress sourceMac, Ip4Address ip4Address) {
        OpenstackPort openstackPort = openstackService.ports().stream()
                .filter(p -> p.macAddress().equals(sourceMac)).iterator().next();
        return openstackPort.fixedIps().values().stream().filter(ip ->
                ip.equals(ip4Address)).count() > 0 ? openstackPort : null;
    }

    private OpenstackFloatingIP getOpenstackFloatingIp(Ip4Address vmIp) {
        Optional<OpenstackFloatingIP> floatingIp = floatingIpMap.asJavaMap().values().stream()
                .filter(f -> f.portId() != null && f.fixedIpAddress().equals(vmIp))
                .findAny();

        if (floatingIp.isPresent()) {
            return floatingIp.get();
        }
        log.debug("There is no floating IP information for VM IP {}", vmIp);

        return null;
    }

    private Optional<OpenstackPort> getRouterInterfacePort(String networkId) {

        return openstackService.ports()
                .stream()
                .filter(p -> p.deviceOwner().equals(DEVICE_OWNER_ROUTER_INTERFACE)
                        && p.networkId().equals(networkId))
                .findAny();
    }

    // TODO: Remove the function and the related codes when vRouter is running on different ONOS instance.
    private void registerFloatingIpToHostService(OpenstackFloatingIP openstackFloatingIp, Action action) {

        Optional<Host> hostOptional = hostService.getHostsByIp(openstackFloatingIp.fixedIpAddress())
                .stream()
                .findFirst();
        if (!hostOptional.isPresent()) {
            log.warn("No host with IP {} is registered and cannot add the floating IP. ",
                    openstackFloatingIp.floatingIpAddress());
            return;
        }

        Host host = hostOptional.get();
        Set<IpAddress> ipAddresses = Sets.newHashSet();
        if (action == Action.ASSOCIATE) {
            ipAddresses.add(openstackFloatingIp.floatingIpAddress());
        }

        HostDescription hostDescription =
                new DefaultHostDescription(host.mac(), host.vlan(), host.location(), ipAddresses,
                        (DefaultAnnotations) host.annotations());

        hostProviderService.hostDetected(host.id(), hostDescription, false);
    }

    private class InternalHostListener implements HostListener {

        private void hostDetected(Host host) {
            String portId = host.annotations().value(Constants.PORT_ID);
            OpenstackPort openstackPort = openstackService.port(portId);
            if (openstackPort == null) {
                log.warn("No OpenstackPort information found from OpenStack for port ID {}", portId);
                return;
            }

            Optional<OpenstackPort> routerPort = getRouterInterfacePort(openstackPort.networkId());
            if (routerPort.isPresent()) {
                OpenstackRouterInterface routerInterface = portToRouterInterface(routerPort.get());
                l3EventExecutorService.execute(() ->
                        setL3Connection(getOpenstackRouter(routerInterface.id()), openstackPort));

            }
        }

        private void hostRemoved(Host host) {
            String portId = host.annotations().value(Constants.PORT_ID);
            OpenstackPort openstackPort = openstackService.port(portId);
            if (openstackPort == null) {
                log.warn("No OpenstackPort information found from OpenStack for port ID {}", portId);
                return;
            }

            Optional<OpenstackPort> routerPort = getRouterInterfacePort(openstackPort.networkId());
            if (routerPort.isPresent()) {
                OpenstackRouterInterface routerInterface = portToRouterInterface(routerPort.get());
                IpAddress ipAddress = host.ipAddresses().stream().findFirst().get();
                l3EventExecutorService.execute(() -> rulePopulator.removeL3Rules(ipAddress.getIp4Address(),
                        getL3ConnectionList(host.annotations().value(Constants.NETWORK_ID),
                                getOpenstackRouterInterface(getOpenstackRouter(routerInterface.id())))));
            }
        }

        private boolean isValidHost(Host host) {
            return !host.ipAddresses().isEmpty() &&
                    host.annotations().value(Constants.VXLAN_ID) != null &&
                    host.annotations().value(Constants.NETWORK_ID) != null &&
                    host.annotations().value(Constants.TENANT_ID) != null &&
                    host.annotations().value(Constants.PORT_ID) != null;
        }

        @Override
        public void event(HostEvent event) {
            Host host = event.subject();
            if (!mastershipService.isLocalMaster(host.location().deviceId())) {
                // do not allow to proceed without mastership
                return;
            }

            if (!isValidHost(host)) {
                log.debug("Invalid host event, ignore it {}", host);
                return;
            }

            switch (event.type()) {
                case HOST_UPDATED:
                case HOST_ADDED:
                    l3EventExecutorService.execute(() -> hostDetected(host));
                    break;
                case HOST_REMOVED:
                    l3EventExecutorService.execute(() -> hostRemoved(host));
                    break;
                default:
                    break;
            }
        }
    }

    private class InternalOpenstackNodeListener implements OpenstackNodeListener {

        private void nodeComplete() {

            rulePopulator = new OpenstackRoutingRulePopulator(appId, openstackService, flowObjectiveService,
                    deviceService, driverService, nodeService, gatewayService);
            openstackIcmpHandler = new OpenstackIcmpHandler(packetService, deviceService, hostService,
                    openstackService, nodeService, gatewayService);
            openstackArpHandler = new OpenstackRoutingArpHandler(packetService, openstackService, nodeService,
                    gatewayService);

            // Packet handlers must be started AFTER all initialization processes.
            packetService.addProcessor(internalPacketProcessor, PacketProcessor.director(1));

            openstackIcmpHandler.requestPacket(appId);
            openstackArpHandler.requestPacket(appId);

            openstackService.floatingIps().stream()
                    .forEach(f -> floatingIpMap.put(f.id(), f));

            reloadInitL3Rules();
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode node = event.node();

            switch (event.type()) {
                case COMPLETE:
                    log.info("COMPLETE node {} detected", node.hostname());
                    l3EventExecutorService.execute(() -> nodeComplete());
                    break;
                case INCOMPLETE:
                    break;
                default:
                    break;
            }
        }
    }

    private class InternalHostProvider extends AbstractProvider implements HostProvider {

        /**
         * Creates a provider with the supplier identifier.
         */
        protected InternalHostProvider() {
            super(PID);
        }

        @Override
        public void triggerProbe(Host host) {
            // nothing to do
        }
    }
}
