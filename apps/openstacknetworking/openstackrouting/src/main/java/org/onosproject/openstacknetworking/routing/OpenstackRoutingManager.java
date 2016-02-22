/*
 * Copyright 2016 Open Networking Laboratory
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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.UDP;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstackinterface.OpenstackFloatingIP;
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.openstackinterface.OpenstackRouter;
import org.onosproject.openstackinterface.OpenstackRouterInterface;
import org.onosproject.openstacknetworking.OpenstackPortInfo;
import org.onosproject.openstacknetworking.OpenstackRoutingService;
import org.onosproject.openstacknetworking.OpenstackSwitchingService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.groupedThreads;

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
    protected OpenstackSwitchingService openstackSwitchingService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ApplicationId appId;
    private ConsistentMap<Integer, String> tpPortNumMap; // Map<PortNum, allocated VM`s Mac & destionation Ip address>
    private ConsistentMap<String, OpenstackFloatingIP> floatingIpMap; // Map<FloatingIp`s Id, FloatingIp object>
    private static final String APP_ID = "org.onosproject.openstackrouting";
    private static final String PORT_NAME = "portName";
    private static final String PORTNAME_PREFIX_VM = "tap";
    private static final String DEVICE_OWNER_ROUTER_INTERFACE = "network:router_interface";
    private static final String FLOATING_IP_MAP_NAME = "openstackrouting-floatingip";
    private static final String TP_PORT_MAP_NAME = "openstackrouting-portnum";
    private static final int PNAT_PORT_EXPIRE_TIME = 1200 * 1000;
    private static final int TP_PORT_MINIMUM_NUM = 1024;
    private static final int TP_PORT_MAXIMUM_NUM = 65535;
    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, OpenstackRoutingConfig.class, "openstackrouting") {
                @Override
                public OpenstackRoutingConfig createConfig() {
                    return new OpenstackRoutingConfig();
                }
            };
    private final NetworkConfigListener configListener = new InternalConfigListener();

    private OpenstackRoutingConfig config;
    private static final KryoNamespace.Builder FLOATING_IP_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(KryoNamespaces.MISC)
            .register(OpenstackFloatingIP.FloatingIpStatus.class)
            .register(OpenstackFloatingIP.class)
            .register(Ip4Address.class)
            .register(String.class);

    private static final KryoNamespace.Builder NUMBER_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(KryoNamespaces.MISC)
            .register(Integer.class)
            .register(String.class);

    private InternalPacketProcessor internalPacketProcessor = new InternalPacketProcessor();
    private ExecutorService l3EventExecutorService =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackrouting", "L3-event"));
    private ExecutorService icmpEventExecutorService =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackrouting", "icmp-event"));
    private ExecutorService arpEventExecutorService =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackrouting", "arp-event"));
    private OpenstackIcmpHandler openstackIcmpHandler;
    private OpenstackRoutingArpHandler openstackArpHandler;
    private OpenstackRoutingRulePopulator rulePopulator;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_ID);
        packetService.addProcessor(internalPacketProcessor, PacketProcessor.director(1));
        configRegistry.registerConfigFactory(configFactory);
        configService.addListener(configListener);

        readConfiguration();

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

        log.info("onos-openstackrouting started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(internalPacketProcessor);
        l3EventExecutorService.shutdown();
        icmpEventExecutorService.shutdown();
        arpEventExecutorService.shutdown();

        floatingIpMap.clear();
        tpPortNumMap.clear();

        log.info("onos-openstackrouting stopped");
    }


    @Override
    public void createFloatingIP(OpenstackFloatingIP openstackFloatingIP) {
        floatingIpMap.put(openstackFloatingIP.id(), openstackFloatingIP);
    }

    @Override
    public void updateFloatingIP(OpenstackFloatingIP openstackFloatingIP) {
        if (!floatingIpMap.containsKey(openstackFloatingIP.id())) {
            log.warn("There`s no information about {} in FloatingIpMap", openstackFloatingIP.id());
            return;
        }
        if (openstackFloatingIP.portId() == null || openstackFloatingIP.portId().equals("null")) {
            OpenstackFloatingIP floatingIP = floatingIpMap.get(openstackFloatingIP.id()).value();
            OpenstackPortInfo portInfo = openstackSwitchingService.openstackPortInfo()
                    .get(PORTNAME_PREFIX_VM.concat(floatingIP.portId().substring(0, 11)));
            if (portInfo == null) {
                log.warn("There`s no portInfo information about portId {}", floatingIP.portId());
                return;
            }
            l3EventExecutorService.execute(
                    new OpenstackFloatingIPHandler(rulePopulator, floatingIP, false, portInfo));
            floatingIpMap.replace(floatingIP.id(), openstackFloatingIP);
        } else {
            floatingIpMap.put(openstackFloatingIP.id(), openstackFloatingIP);
            l3EventExecutorService.execute(
                    new OpenstackFloatingIPHandler(rulePopulator, openstackFloatingIP, true, null));
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
            Ip4Address externalIp = openstackRouter.gatewayExternalInfo().externalFixedIps()
                    .values().stream().findFirst().orElse(null);
            OpenstackRouter router = getRouterfromExternalIp(externalIp);
            checkExternalConnection(router, getOpenstackRouterInterface(router));
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
    }

    @Override
    public void removeRouterInterface(OpenstackRouterInterface routerInterface) {
        rulePopulator.removeExternalRules(routerInterface);
    }

    @Override
    public void checkDisassociatedFloatingIp(String portId, OpenstackPortInfo portInfo) {
        if (floatingIpMap.size() < 1) {
            log.info("No information in FloatingIpMap");
            return;
        }
        OpenstackFloatingIP floatingIp = associatedFloatingIps()
                .stream()
                .filter(fIp -> fIp.portId().equals(portId))
                .findAny()
                .orElse(null);
        if (floatingIp != null && portInfo != null) {
            l3EventExecutorService.execute(
                    new OpenstackFloatingIPHandler(rulePopulator, floatingIp, false, portInfo));
            OpenstackFloatingIP.Builder fBuilder = new OpenstackFloatingIP.Builder()
                    .floatingIpAddress(floatingIp.floatingIpAddress())
                    .id(floatingIp.id())
                    .networkId(floatingIp.networkId())
                    .status(floatingIp.status())
                    .tenantId(floatingIp.tenantId());
            floatingIpMap.replace(floatingIp.id(), fBuilder.build());
        } else if (portInfo == null) {
            log.warn("portInfo is null as timing issue between ovs port update event and openstack deletePort event");
        }
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
        l3EventExecutorService.submit(() ->
                        openstackService.ports()
                                .stream()
                                .filter(p -> p.deviceOwner().equals(DEVICE_OWNER_ROUTER_INTERFACE))
                                .forEach(p -> {
                                    OpenstackRouterInterface routerInterface = portToRouterInterface(p);
                                    updateRouterInterface(routerInterface);
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

            if (context.isHandled()) {
                return;
            } else if (!context.inPacket().receivedFrom().deviceId().toString()
                    .equals(config.gatewayBridgeId())) {
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

                        icmpEventExecutorService.submit(() ->
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
                        Port port =
                                getExternalPort(pkt.receivedFrom().deviceId(), config.gatewayExternalInterfaceName());
                        if (port == null) {
                            log.warn("There`s no external interface");
                            break;
                        }
                        OpenstackPort openstackPort = getOpenstackPort(ethernet.getSourceMAC(),
                                Ip4Address.valueOf(iPacket.getSourceAddress()));
                        l3EventExecutorService.execute(new OpenstackPnatHandler(rulePopulator, context,
                                portNum, openstackPort, port, config));
                        break;
                }
            } else if (ethernet.getEtherType() == Ethernet.TYPE_ARP) {
                arpEventExecutorService.submit(() ->
                        openstackArpHandler.processArpPacketFromRouter(context, ethernet));
            }
        }

        private int getPortNum(MacAddress sourceMac, int destinationAddress) {
            int portNum = findUnusedPortNum();
            if (portNum == 0) {
                clearPortNumMap();
                portNum = findUnusedPortNum();
            }
            tpPortNumMap.put(portNum, sourceMac.toString().concat(":").concat(String.valueOf(destinationAddress)));
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

    private void clearPortNumMap() {
        tpPortNumMap.entrySet().forEach(e -> {
            if (System.currentTimeMillis() - e.getValue().creationTime() > PNAT_PORT_EXPIRE_TIME) {
                tpPortNumMap.remove(e.getKey());
            }
        });
    }

    private Port getExternalPort(DeviceId deviceId, String interfaceName) {
        return deviceService.getPorts(deviceId)
                .stream()
                .filter(p -> p.annotations().value(PORT_NAME).equals(interfaceName))
                .findAny()
                .orElse(null);
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
        if (router.id() == null) {
            interfaces.forEach(i -> initiateL3Rule(getRouterfromExternalIp(externalIp), i));
        } else {
            interfaces.forEach(i -> initiateL3Rule(router, i));
        }

    }

    private OpenstackRouter getRouterfromExternalIp(Ip4Address externalIp) {
        OpenstackRouter router = getExternalRouter(true)
                .stream()
                .filter(r -> r.gatewayExternalInfo()
                        .externalFixedIps()
                        .values()
                        .stream()
                        .findFirst()
                        .orElse(null)
                        .equals(externalIp))
                .findAny()
                .orElse(null);
        return checkNotNull(router);
    }

    private void initiateL3Rule(OpenstackRouter router, OpenstackRouterInterface routerInterface) {
        long vni = Long.parseLong(openstackService.network(openstackService
                .port(routerInterface.portId()).networkId()).segmentId());
        rulePopulator.populateExternalRules(vni, router, routerInterface);
    }

    private Collection<OpenstackRouterInterface> getOpenstackRouterInterface(OpenstackRouter router) {
        List<OpenstackRouterInterface> interfaces = Lists.newArrayList();
        openstackService.ports()
                .stream()
                .filter(p -> p.deviceOwner().equals(DEVICE_OWNER_ROUTER_INTERFACE))
                .filter(p -> p.deviceId().equals(router.id()))
                .forEach(p -> {
                    OpenstackRouterInterface routerInterface = portToRouterInterface(p);
                    interfaces.add(routerInterface);
                });
        return interfaces;
    }

    private OpenstackRouter getOpenstackRouter(String id) {
        return openstackService.routers().stream().filter(r ->
                r.id().equals(id)).findAny().orElse(null);
    }

    private OpenstackPort getOpenstackPort(MacAddress sourceMac, Ip4Address ip4Address) {
        OpenstackPort openstackPort = openstackService.ports().stream()
                .filter(p -> p.macAddress().equals(sourceMac)).findFirst().orElse(null);
        return openstackPort.fixedIps().values().stream().filter(ip ->
                ip.equals(ip4Address)).count() > 0 ? openstackPort : null;
    }

    private void readConfiguration() {
        config = configService.getConfig(appId, OpenstackRoutingConfig.class);
        if (config == null) {
            log.error("No configuration found");
            return;
        }

        checkNotNull(config.physicalRouterMac());
        checkNotNull(config.gatewayBridgeId());
        checkNotNull(config.gatewayExternalInterfaceMac());
        checkNotNull(config.gatewayExternalInterfaceName());

        log.debug("Configured info: {}, {}, {}, {}", config.physicalRouterMac(), config.gatewayBridgeId(),
                config.gatewayExternalInterfaceMac(), config.gatewayExternalInterfaceName());

        rulePopulator = new OpenstackRoutingRulePopulator(appId,
                openstackService, flowObjectiveService, deviceService, driverService, config);

        openstackIcmpHandler = new OpenstackIcmpHandler(packetService, deviceService,
                openstackService, config, openstackSwitchingService);
        openstackArpHandler = new OpenstackRoutingArpHandler(packetService, openstackService, config);

        openstackIcmpHandler.requestPacket(appId);
        openstackArpHandler.requestPacket(appId);
        reloadInitL3Rules();
        log.info("OpenstackRouting configured");
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(OpenstackRoutingConfig.class)) {
                return;
            }

            if (event.type().equals(NetworkConfigEvent.Type.CONFIG_ADDED) ||
                    event.type().equals(NetworkConfigEvent.Type.CONFIG_UPDATED)) {
                l3EventExecutorService.execute(OpenstackRoutingManager.this::readConfiguration);
                rulePopulator = new OpenstackRoutingRulePopulator(appId,
                        openstackService, flowObjectiveService, deviceService, driverService, config);

            }
        }
    }
}
