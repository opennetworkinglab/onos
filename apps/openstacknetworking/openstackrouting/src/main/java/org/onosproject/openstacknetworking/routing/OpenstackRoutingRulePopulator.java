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

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TCP;
import org.onlab.packet.TpPort;
import org.onlab.packet.UDP;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.openstacknetworking.OpenstackNetworkingService;
import org.onosproject.openstacknetworking.OpenstackPort;
import org.onosproject.openstacknetworking.OpenstackRouter;
import org.onosproject.openstacknetworking.OpenstackRouterInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Populates Routing Flow Rules.
 */
public class OpenstackRoutingRulePopulator {

    private static Logger log = LoggerFactory
            .getLogger(OpenstackRoutingRulePopulator.class);
    private ApplicationId appId;
    private FlowObjectiveService flowObjectiveService;
    private OpenstackNetworkingService openstackService;
    private DeviceService deviceService;
    private DriverService driverService;

    public static final String PORTNAME_PREFIX_VM = "tap";
    public static final String PORTNAME_PREFIX_ROUTER = "qr";
    public static final String PORTNAME_PREFIX_TUNNEL = "vxlan";
    public static final String PORTNAME = "portName";

    private static final int ROUTING_RULE_PRIORITY = 25000;
    private static final int PNAT_RULE_PRIORITY = 24000;
    private static final int PNAT_TIMEOUT = 120;
    private static final MacAddress GATEWAYMAC = MacAddress.valueOf("1f:1f:1f:1f:1f:1f");

    private InboundPacket inboundPacket;
    private OpenstackPort openstackPort;
    private int portNum;
    private MacAddress externalInterface;
    private MacAddress externalRouter;
    private OpenstackRouter router;
    private OpenstackRouterInterface routerInterface;

    // TODO: This will be replaced to get the information from openstackswitchingservice.
    private static final String EXTERNAL_INTERFACE_NAME = "eth3";

    public OpenstackRoutingRulePopulator(ApplicationId appId, OpenstackNetworkingService openstackService,
                                         FlowObjectiveService flowObjectiveService,
                                         DeviceService deviceService, DriverService driverService) {
        this.appId = appId;
        this.flowObjectiveService = flowObjectiveService;
        this.openstackService = openstackService;
        this.deviceService = deviceService;
        this.driverService = driverService;
    }

    public void populatePnatFlowRules(InboundPacket inboundPacket, OpenstackPort openstackPort, int portNum,
                                      MacAddress externalInterfaceMacAddress, MacAddress externalRouterMacAddress) {
        this.inboundPacket = inboundPacket;
        this.openstackPort = openstackPort;
        this.portNum = portNum;
        this.externalInterface = externalInterfaceMacAddress;
        this.externalRouter = externalRouterMacAddress;

        long vni = getVni(openstackPort);

        populatePnatIncomingFlowRules(vni);
        populatePnatOutgoingFlowRules(vni);
    }

    private void populatePnatOutgoingFlowRules(long vni) {
        IPv4 iPacket = (IPv4) inboundPacket.parsed().getPayload();

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(iPacket.getProtocol())
                .matchTunnelId(vni)
                .matchIPSrc(IpPrefix.valueOf(iPacket.getSourceAddress(), 32))
                .matchIPDst(IpPrefix.valueOf(iPacket.getDestinationAddress(), 32));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.setEthSrc(externalInterface)
                .setEthDst(externalRouter);

        switch (iPacket.getProtocol()) {
            case IPv4.PROTOCOL_TCP:
                TCP tcpPacket = (TCP) iPacket.getPayload();
                sBuilder.matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
                        .matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
                tBuilder.setTcpDst(TpPort.tpPort(portNum));
                break;
            case IPv4.PROTOCOL_UDP:
                UDP udpPacket = (UDP) iPacket.getPayload();
                sBuilder.matchUdpDst(TpPort.tpPort(udpPacket.getSourcePort()))
                        .matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
                tBuilder.setUdpDst(TpPort.tpPort(portNum));
                break;
            default:
                break;
        }

        Port port = getPortNumOfExternalInterface();
        checkNotNull(port, "Port can not be null");
        tBuilder.setOutput(port.number());

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PNAT_RULE_PRIORITY)
                .makeTemporary(PNAT_TIMEOUT)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(inboundPacket.receivedFrom().deviceId(), fo);
    }

    private Port getPortNumOfExternalInterface() {
        return deviceService.getPorts(inboundPacket.receivedFrom().deviceId()).stream()
                .filter(p -> p.annotations().value("portName").equals(EXTERNAL_INTERFACE_NAME))
                .findAny().orElse(null);
    }


    private void populatePnatIncomingFlowRules(long vni) {
        IPv4 iPacket = (IPv4) inboundPacket.parsed().getPayload();
        DeviceId deviceId = inboundPacket.receivedFrom().deviceId();

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(iPacket.getProtocol())
                .matchIPSrc(IpPrefix.valueOf(iPacket.getDestinationAddress(), 32));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.setTunnelId(vni)
                .setIpDst(IpAddress.valueOf(iPacket.getSourceAddress()));

        switch (iPacket.getProtocol()) {
            case IPv4.PROTOCOL_TCP:
                TCP tcpPacket = (TCP) iPacket.getPayload();
                sBuilder.matchTcpSrc(TpPort.tpPort(tcpPacket.getDestinationPort()))
                        .matchTcpDst(TpPort.tpPort(portNum));
                tBuilder.setTcpDst(TpPort.tpPort(tcpPacket.getSourcePort()));
                break;
            case IPv4.PROTOCOL_UDP:
                UDP udpPacket = (UDP) iPacket.getPayload();
                sBuilder.matchUdpSrc(TpPort.tpPort(udpPacket.getDestinationPort()))
                        .matchUdpDst(TpPort.tpPort(portNum));
                tBuilder.setUdpDst(TpPort.tpPort(udpPacket.getSourcePort()));
                break;
            default:
                break;
        }

        tBuilder.extension(buildNiciraExtenstion(deviceId, Ip4Address.valueOf(iPacket.getSourceAddress())), deviceId)
                .setOutput(getTunnelPort(deviceId));

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PNAT_RULE_PRIORITY)
                .makeTemporary(PNAT_TIMEOUT)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(inboundPacket.receivedFrom().deviceId(), fo);
    }

    private ExtensionTreatment buildNiciraExtenstion(DeviceId id, Ip4Address hostIp) {
        Driver driver = driverService.getDriver(id);
        DriverHandler driverHandler = new DefaultDriverHandler(new DefaultDriverData(driver, id));
        ExtensionTreatmentResolver resolver = driverHandler.behaviour(ExtensionTreatmentResolver.class);

        ExtensionTreatment extensionInstruction =
                resolver.getExtensionInstruction(
                        ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST.type());

        try {
            extensionInstruction.setPropertyValue("tunnelDst", hostIp);
        } catch (ExtensionPropertyException e) {
            log.error("Error setting Nicira extension setting {}", e);
        }

        return extensionInstruction;
    }

    private PortNumber getTunnelPort(DeviceId deviceId) {
        Port port = deviceService.getPorts(deviceId).stream()
                .filter(p -> p.annotations().value("portName").equals(PORTNAME_PREFIX_TUNNEL))
                .findAny().orElse(null);

        if (port == null) {
            log.error("No TunnelPort was created.");
            return null;
        }
        return port.number();

    }

    public void populateExternalRules(long vni, OpenstackRouter router,
                                      OpenstackRouterInterface routerInterface) {
        this.router = router;
        this.routerInterface = routerInterface;

        // 1. computeNode to gateway
        populateComputeNodeRules(vni);
        // 2. gatewayNode to controller
        populateRuleGatewaytoController(vni);
    }

    private void populateRuleGatewaytoController(long vni) {
        Device gatewayDevice = getGatewayNode();
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(vni)
                .matchEthDst(GATEWAYMAC);
        tBuilder.setOutput(PortNumber.CONTROLLER);

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(ROUTING_RULE_PRIORITY)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(gatewayDevice.id(), fo);
    }

    private void populateComputeNodeRules(long vni) {
        Device gatewayDevice = getGatewayNode();

        StreamSupport.stream(deviceService.getAvailableDevices().spliterator(), false)
                .filter(d -> !checkGatewayNode(d.id()))
                .forEach(d -> populateRuleToGateway(d, gatewayDevice, vni));
        /*deviceService.getAvailableDevices().forEach(d -> {
            if (!checkGatewayNode(d.id())) {
                populateRuleToGateway(d, gatewayDevice, vni);
            }
        });*/
    }

    private void populateRuleToGateway(Device d, Device gatewayDevice, long vni) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(vni)
                .matchEthDst(GATEWAYMAC);
        tBuilder.extension(buildNiciraExtenstion(d.id(), getIPAddressforDevice(gatewayDevice)), d.id())
                .setOutput(getTunnelPort(d.id()));

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .withPriority(ROUTING_RULE_PRIORITY)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(d.id(), fo);
    }

    private Ip4Address getIPAddressforDevice(Device device) {
        return Ip4Address.valueOf(device.annotations().value("channelId").split(":")[0]);
    }

    private Device getGatewayNode() {
        final Device[] device = new Device[1];
        deviceService.getAvailableDevices().forEach(d -> {
            if (checkGatewayNode(d.id())) {
                device[0] = d;
            }
        });
        return device[0];
    }

    private boolean checkGatewayNode(DeviceId deviceId) {
        return !deviceService.getPorts(deviceId).stream().anyMatch(port ->
                port.annotations().value("portName").startsWith(PORTNAME_PREFIX_ROUTER) ||
                        port.annotations().value("portName").startsWith(PORTNAME_PREFIX_VM));
    }

    private long getVni(OpenstackPort openstackPort) {
        return Long.parseLong(openstackService.network(openstackPort.networkId()).segmentId());
    }

    public void removeExternalRules(OpenstackRouterInterface routerInterface) {
        OpenstackPort openstackPort = openstackService.port(routerInterface.portId());
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(getVni(openstackPort))
                .matchEthDst(GATEWAYMAC);

        StreamSupport.stream(deviceService.getAvailableDevices().spliterator(), false)
                .forEach(d -> {
                    if (checkGatewayNode(d.id())) {
                        removeExternalRule(d.id(), sBuilder, ForwardingObjective.Flag.VERSATILE);
                    } else {
                        removeExternalRule(d.id(), sBuilder, ForwardingObjective.Flag.SPECIFIC);
                    }
                });

    }

    private void removeExternalRule(DeviceId id, TrafficSelector.Builder sBuilder, ForwardingObjective.Flag flag) {
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withFlag(flag)
                .withPriority(ROUTING_RULE_PRIORITY)
                .fromApp(appId)
                .remove();

        flowObjectiveService.forward(id, fo);
    }

}
