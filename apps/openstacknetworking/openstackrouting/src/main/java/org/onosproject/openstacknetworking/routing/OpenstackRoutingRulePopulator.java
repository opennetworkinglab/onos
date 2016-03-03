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
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.openstackinterface.OpenstackRouter;
import org.onosproject.openstackinterface.OpenstackRouterInterface;
import org.onosproject.openstackinterface.OpenstackSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Populates Routing Flow Rules.
 */
public class OpenstackRoutingRulePopulator {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ApplicationId appId;
    private final FlowObjectiveService flowObjectiveService;
    private final OpenstackInterfaceService openstackService;
    private final DeviceService deviceService;
    private final DriverService driverService;

    private static final String PORTNAME_PREFIX_VM = "tap";
    private static final String PORTNAME_PREFIX_ROUTER = "qr";
    private static final String PORTNAME_PREFIX_TUNNEL = "vxlan";
    private static final String PORTNAME = "portName";

    private static final String PORTNOTNULL = "Port can not be null";
    private static final String TUNNEL_DESTINATION = "tunnelDst";
    private static final String DEVICE_ANNOTATION_CHANNELID = "channelId";
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
    private static final String EXTERNAL_INTERFACE_NAME = "veth0";

    /**
     * The constructor of openstackRoutingRulePopulator.
     *
     * @param appId Caller`s appId
     * @param openstackService OpenstackNetworkingService
     * @param flowObjectiveService FlowObjectiveService
     * @param deviceService DeviceService
     * @param driverService DriverService
     */
    public OpenstackRoutingRulePopulator(ApplicationId appId, OpenstackInterfaceService openstackService,
                                         FlowObjectiveService flowObjectiveService,
                                         DeviceService deviceService, DriverService driverService) {
        this.appId = appId;
        this.flowObjectiveService = flowObjectiveService;
        this.openstackService = openstackService;
        this.deviceService = deviceService;
        this.driverService = driverService;
    }

    /**
     * Populates flow rules for Pnat configurations.
     *  @param inboundPacket Packet-in event packet
     * @param openstackPort Target VM information
     * @param portNum Pnat port number
     * @param externalIp external ip address
     * @param externalInterfaceMacAddress Gateway external interface macaddress
     * @param externalRouterMacAddress Outer(physical) router`s macaddress
     */
    public void populatePnatFlowRules(InboundPacket inboundPacket, OpenstackPort openstackPort, int portNum,
                                      Ip4Address externalIp, MacAddress externalInterfaceMacAddress,
                                      MacAddress externalRouterMacAddress) {
        this.inboundPacket = inboundPacket;
        this.openstackPort = openstackPort;
        this.portNum = portNum;
        this.externalInterface = externalInterfaceMacAddress;
        this.externalRouter = externalRouterMacAddress;

        long vni = getVni(openstackPort);

        populatePnatIncomingFlowRules(vni, externalIp);
        populatePnatOutgoingFlowRules(vni, externalIp);
    }

    private void populatePnatOutgoingFlowRules(long vni, Ip4Address externalIp) {
        IPv4 iPacket = (IPv4) inboundPacket.parsed().getPayload();

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(iPacket.getProtocol())
                .matchTunnelId(vni)
                .matchIPSrc(IpPrefix.valueOf(iPacket.getSourceAddress(), 32))
                .matchIPDst(IpPrefix.valueOf(iPacket.getDestinationAddress(), 32));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.setEthSrc(externalInterface)
                .setEthDst(externalRouter)
                .setIpSrc(externalIp);

        switch (iPacket.getProtocol()) {
            case IPv4.PROTOCOL_TCP:
                TCP tcpPacket = (TCP) iPacket.getPayload();
                sBuilder.matchTcpSrc(TpPort.tpPort(tcpPacket.getSourcePort()))
                        .matchTcpDst(TpPort.tpPort(tcpPacket.getDestinationPort()));
                tBuilder.setTcpSrc(TpPort.tpPort(portNum));
                break;
            case IPv4.PROTOCOL_UDP:
                UDP udpPacket = (UDP) iPacket.getPayload();
                sBuilder.matchUdpSrc(TpPort.tpPort(udpPacket.getSourcePort()))
                        .matchUdpDst(TpPort.tpPort(udpPacket.getDestinationPort()));
                tBuilder.setUdpSrc(TpPort.tpPort(portNum));
                break;
            default:
                break;
        }

        Port port = checkNotNull(getPortNumOfExternalInterface(), PORTNOTNULL);
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
                .filter(p -> p.annotations().value(PORTNAME).equals(EXTERNAL_INTERFACE_NAME))
                .findAny().orElse(null);
    }


    private void populatePnatIncomingFlowRules(long vni, Ip4Address externalIp) {
        IPv4 iPacket = (IPv4) inboundPacket.parsed().getPayload();
        DeviceId deviceId = inboundPacket.receivedFrom().deviceId();

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(iPacket.getProtocol())
                .matchIPDst(IpPrefix.valueOf(externalIp, 32))
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
            extensionInstruction.setPropertyValue(TUNNEL_DESTINATION, hostIp);
        } catch (ExtensionPropertyException e) {
            log.error("Error setting Nicira extension setting {}", e);
        }

        return extensionInstruction;
    }

    private PortNumber getTunnelPort(DeviceId deviceId) {
        Port port = deviceService.getPorts(deviceId).stream()
                .filter(p -> p.annotations().value(PORTNAME).equals(PORTNAME_PREFIX_TUNNEL))
                .findAny().orElse(null);

        if (port == null) {
            log.error("No TunnelPort was created.");
            return null;
        }
        return port.number();

    }

    /**
     * Populates flow rules from openstackComputeNode to GatewayNode.
     *
     * @param vni Target network
     * @param router corresponding router
     * @param routerInterface corresponding routerInterface
     */
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
        return Ip4Address.valueOf(device.annotations().value(DEVICE_ANNOTATION_CHANNELID).split(":")[0]);
    }

    private Device getGatewayNode() {
        return checkNotNull(StreamSupport.stream(deviceService.getAvailableDevices().spliterator(), false)
                .filter(d -> checkGatewayNode(d.id()))
                .findAny()
                .orElse(null));
    }

    private boolean checkGatewayNode(DeviceId deviceId) {
        return !deviceService.getPorts(deviceId).stream().anyMatch(port ->
                port.annotations().value(PORTNAME).startsWith(PORTNAME_PREFIX_ROUTER) ||
                        port.annotations().value(PORTNAME).startsWith(PORTNAME_PREFIX_VM));
    }

    private long getVni(OpenstackPort openstackPort) {
        return Long.parseLong(openstackService.network(openstackPort.networkId()).segmentId());
    }

    private long getVni(OpenstackSubnet openstackSubnet) {
        return Long.parseLong(openstackService.network(openstackSubnet.networkId()).segmentId());
    }

    /**
     * Remove flow rules for external connection.
     *
     * @param routerInterface Corresponding routerInterface
     */
    public void removeExternalRules(OpenstackRouterInterface routerInterface) {
        OpenstackSubnet openstackSubnet = openstackService.subnet(routerInterface.subnetId());
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(getVni(openstackSubnet))
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
