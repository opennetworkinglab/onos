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
import com.google.common.collect.Maps;
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
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
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
import org.onosproject.openstacknetworking.OpenstackRoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    private ApplicationId appId;
    private Map<String, OpenstackRouterInterface> routerInterfaceMap = Maps.newHashMap();
    private Map<Integer, String> portNumMap = initPortNumMap();
    private static final String APP_ID = "org.onosproject.openstackrouting";
    private static final String PORT_NAME = "portName";
    private static final String DEVICE_OWNER_ROUTER_INTERFACE = "network:router_interface";

    // TODO: This will be replaced to get the information from openstackswitchingservice.
    private static final String EXTERNAL_INTERFACE_NAME = "veth0";

    private Map<Integer, String> initPortNumMap() {
        Map<Integer, String> map = Maps.newHashMap();
        for (int i = 1024; i < 65535; i++) {
            map.put(i, "");
        }
        return map;
    }

    private InternalPacketProcessor internalPacketProcessor = new InternalPacketProcessor();
    private ExecutorService l3EventExecutorService =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackrouting", "L3-event"));
    private ExecutorService icmpEventExecutorService =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackrouting", "icmp-event"));

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_ID);
        packetService.addProcessor(internalPacketProcessor, PacketProcessor.director(1));
        log.info("onos-openstackrouting started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(internalPacketProcessor);
        log.info("onos-openstackrouting stopped");
    }


    @Override
    public void createFloatingIP(OpenstackFloatingIP openstackFloatingIP) {

    }

    @Override
    public void updateFloatingIP(OpenstackFloatingIP openstackFloatingIP) {

    }

    @Override
    public void deleteFloatingIP(String id) {

    }

    @Override
    public void createRouter(OpenstackRouter openstackRouter) {
        checkExternalConnection(openstackRouter, getOpenstackRouterInterface(openstackRouter));
    }

    @Override
    public void updateRouter(OpenstackRouter openstackRouter) {
        checkExternalConnection(openstackRouter, getOpenstackRouterInterface(openstackRouter));
    }

    @Override
    public void deleteRouter(String id) {
        //TODO
    }

    @Override
    public void updateRouterInterface(OpenstackRouterInterface routerInterface) {
        routerInterfaceMap.putIfAbsent(routerInterface.portId(), routerInterface);
        List<OpenstackRouterInterface> routerInterfaces = Lists.newArrayList();
        routerInterfaces.add(routerInterface);
        checkExternalConnection(getOpenstackRouter(routerInterface.portId()), routerInterfaces);
    }

    @Override
    public void removeRouterInterface(OpenstackRouterInterface routerInterface) {
        OpenstackRoutingRulePopulator rulePopulator = new OpenstackRoutingRulePopulator(appId,
                openstackService, flowObjectiveService, deviceService, driverService);
        rulePopulator.removeExternalRules(routerInterface);
        routerInterfaceMap.remove(routerInterface.portId());
    }

    private void reloadInitL3Rules() {
        openstackService.ports()
                .stream()
                .filter(p -> p.deviceOwner().equals(DEVICE_OWNER_ROUTER_INTERFACE))
                .forEach(p -> {
                    OpenstackRouterInterface routerInterface = portToRouterInterface(p);
                    updateRouterInterface(routerInterface);
                });
    }

    private OpenstackRouterInterface portToRouterInterface(OpenstackPort p) {
        OpenstackRouterInterface.Builder osBuilder = new OpenstackRouterInterface.Builder()
                .id(checkNotNull(p.id()))
                .tenantId(checkNotNull(openstackService.network(p.networkId()).tenantId()))
                .subnetId(checkNotNull(p.fixedIps().keySet().stream().findFirst().orElse(null)).toString())
                .portId(checkNotNull(p.deviceId()));

        return osBuilder.build();
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethernet = pkt.parsed();

            if (ethernet != null && ethernet.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 iPacket = (IPv4) ethernet.getPayload();
                OpenstackRoutingRulePopulator rulePopulator = new OpenstackRoutingRulePopulator(appId,
                        openstackService, flowObjectiveService, deviceService,
                        driverService);
                switch (iPacket.getProtocol()) {
                    case IPv4.PROTOCOL_ICMP:
                        icmpEventExecutorService.execute(new OpenstackIcmpHandler(rulePopulator, context));
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
                        Port port = getExternalPort(pkt.receivedFrom().deviceId(), EXTERNAL_INTERFACE_NAME);
                        if (port == null) {
                            log.warn("There`s no external interface");
                            break;
                        }
                        OpenstackPort openstackPort = getOpenstackPort(ethernet.getSourceMAC(),
                                Ip4Address.valueOf(iPacket.getSourceAddress()));
                        l3EventExecutorService.execute(new OpenstackPnatHandler(rulePopulator, context,
                                portNum, openstackPort, port));
                        break;
                }

            }
        }

        private int getPortNum(MacAddress sourceMac, int destinationAddress) {
            int portNum = portNumMap.keySet().stream()
                    .filter(k -> portNumMap.get(k).equals("")).findFirst().orElse(0);
            portNumMap.replace(portNum, sourceMac.toString().concat(":").concat(String.valueOf(destinationAddress)));
            return portNum;
        }
    }

    private Port getExternalPort(DeviceId deviceId, String interfaceName) {
        return deviceService.getPorts(deviceId)
                .stream()
                .filter(p -> p.annotations().value(PORT_NAME).equals(interfaceName))
                .findAny()
                .orElse(null);
    }

    private void checkExternalConnection(OpenstackRouter router,
                                         Collection<OpenstackRouterInterface> routerInterfaces) {
        checkNotNull(router, "Router can not be null");
        checkNotNull(routerInterfaces, "routerInterfaces can not be null");
        Ip4Address externalIp = router.gatewayExternalInfo().externalFixedIps()
                .values().stream().findFirst().orElse(null);
        if ((externalIp == null) || (!router.gatewayExternalInfo().isEnablePnat())) {
            log.debug("Not satisfied to set pnat configuration");
            return;
        }
        routerInterfaces.forEach(routerInterface -> initiateL3Rule(router, routerInterface));
    }

    private void initiateL3Rule(OpenstackRouter router, OpenstackRouterInterface routerInterface) {
        long vni = Long.parseLong(openstackService.network(openstackService
                .port(routerInterface.id()).networkId()).segmentId());
        OpenstackRoutingRulePopulator rulePopulator = new OpenstackRoutingRulePopulator(appId,
                openstackService, flowObjectiveService, deviceService, driverService);
        rulePopulator.populateExternalRules(vni, router, routerInterface);
    }

    private Collection<OpenstackRouterInterface> getOpenstackRouterInterface(OpenstackRouter router) {
        return routerInterfaceMap.values().stream().filter(i -> i.id().equals(router.id()))
                .collect(Collectors.toList());
    }

    private OpenstackRouter getOpenstackRouter(String id) {
        return openstackService.routers().stream().filter(r ->
                r.id().equals(id)).findAny().orElse(null);
    }

    private OpenstackPort getOpenstackPort(MacAddress sourceMac, Ip4Address ip4Address) {
        OpenstackPort openstackPort = openstackService.ports().stream()
                .filter(p -> p.macAddress().equals(sourceMac)).findFirst().orElse(null);
        return checkNotNull(openstackPort.fixedIps().values().stream().findFirst().orElse(null))
                .equals(ip4Address) ? openstackPort : null;
    }

}
