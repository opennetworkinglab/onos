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
import org.onosproject.openstackinterface.OpenstackRouterInterface;
import org.onosproject.openstackinterface.OpenstackSubnet;
import org.onosproject.openstackinterface.OpenstackFloatingIP;
import org.onosproject.openstacknetworking.OpenstackNetworkingConfig;
import org.onosproject.openstacknetworking.OpenstackPortInfo;
import org.onosproject.openstacknetworking.OpenstackRoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.osgi.DefaultServiceDirectory.getService;

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
    private final OpenstackNetworkingConfig config;

    private static final String PORTNAME_PREFIX_TUNNEL = "vxlan";
    private static final String PORTNAME = "portName";
    private static final String PORTNAME_PREFIX_VM = "tap";

    private static final String PORTNOTNULL = "Port can not be null";
    private static final String DEVICENOTNULL = "Device can not be null";
    private static final String TUNNEL_DESTINATION = "tunnelDst";
    private static final int ROUTING_RULE_PRIORITY = 25000;
    private static final int FLOATING_RULE_PRIORITY = 42000;
    private static final int PNAT_RULE_PRIORITY = 26000;
    private static final int PNAT_TIMEOUT = 120;
    private static final int PREFIX_LENGTH = 32;
    private static final MacAddress GATEWAYMAC = MacAddress.valueOf("1f:1f:1f:1f:1f:1f");

    private InboundPacket inboundPacket;
    private OpenstackPort openstackPort;
    private int portNum;
    private MacAddress externalInterface;
    private MacAddress externalRouter;

    /**
     * The constructor of openstackRoutingRulePopulator.
     *
     * @param appId Caller`s appId
     * @param openstackService Opestack REST request handler
     * @param flowObjectiveService FlowObjectiveService
     * @param deviceService DeviceService
     * @param driverService DriverService
     * @param config Configuration for openstack environment
     */
    public OpenstackRoutingRulePopulator(ApplicationId appId, OpenstackInterfaceService openstackService,
                                         FlowObjectiveService flowObjectiveService, DeviceService deviceService,
                                         DriverService driverService, OpenstackNetworkingConfig config) {
        this.appId = appId;
        this.flowObjectiveService = flowObjectiveService;
        this.openstackService = checkNotNull(openstackService);
        this.deviceService = deviceService;
        this.driverService = driverService;
        this.config = config;
    }

    /**
     * Populates flow rules for Pnat configurations.
     *
     * @param inboundPacket Packet-in event packet
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

        long vni = getVni(openstackPort.networkId());

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
                log.debug("Unsupported IPv4 protocol {}");
                break;
        }

        Port port = checkNotNull(getPortOfExternalInterface(), PORTNOTNULL);
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

    private Port getPortOfExternalInterface() {
        return deviceService.getPorts(getGatewayNode().id()).stream()
                .filter(p -> p.annotations().value(PORTNAME).equals(config.gatewayExternalInterfaceName()))
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
                .setEthDst(inboundPacket.parsed().getSourceMAC())
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

        tBuilder.extension(buildNiciraExtenstion(deviceId, getHostIpfromOpenstackPort(openstackPort)), deviceId)
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

    private Ip4Address getHostIpfromOpenstackPort(OpenstackPort openstackPort) {
        Device device = getDevicefromOpenstackPort(openstackPort);
        return config.nodes().get(device.id());
    }

    private Device getDevicefromOpenstackPort(OpenstackPort openstackPort) {
        String openstackPortName = PORTNAME_PREFIX_VM + openstackPort.id().substring(0, 11);
        Device device = StreamSupport.stream(deviceService.getDevices().spliterator(), false)
                .filter(d -> findPortinDevice(d, openstackPortName))
                .iterator()
                .next();
        checkNotNull(device, DEVICENOTNULL);
        return device;
    }

    private boolean findPortinDevice(Device d, String openstackPortName) {
        Port port = deviceService.getPorts(d.id())
                .stream()
                .filter(p -> p.isEnabled() && p.annotations().value(PORTNAME).equals(openstackPortName))
                .findAny()
                .orElse(null);
        return port != null;
    }

    /**
     * Builds Nicira extension for tagging remoteIp of vxlan.
     *
     * @param id Device Id of vxlan source device
     * @param hostIp Remote Ip of vxlan destination device
     * @return NiciraExtension Treatment
     */
    public ExtensionTreatment buildNiciraExtenstion(DeviceId id, Ip4Address hostIp) {
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

    /**
     * Returns port number of vxlan tunnel.
     *
     * @param deviceId Target Device Id
     * @return PortNumber
     */
    public PortNumber getTunnelPort(DeviceId deviceId) {
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
     */
    public void populateExternalRules(long vni) {

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

        StreamSupport.stream(deviceService.getDevices().spliterator(), false)
                .filter(d -> !checkGatewayNode(d.id()))
                .forEach(d -> populateRuleToGateway(d, gatewayDevice, vni));
    }

    private void populateRuleToGateway(Device d, Device gatewayDevice, long vni) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(vni)
                .matchEthDst(GATEWAYMAC);
        tBuilder.extension(buildNiciraExtenstion(d.id(), config.nodes().get(gatewayDevice.id())), d.id())
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

    private Device getGatewayNode() {
        return checkNotNull(deviceService.getDevice(DeviceId.deviceId(config.gatewayBridgeId())));
    }

    private boolean checkGatewayNode(DeviceId deviceId) {
        return deviceId.toString().equals(config.gatewayBridgeId());
    }

    private long getVni(String netId) {
        return Long.parseLong(openstackService.network(netId).segmentId());
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
                .matchTunnelId(getVni(openstackSubnet.networkId()))
                .matchEthDst(GATEWAYMAC);

        StreamSupport.stream(deviceService.getDevices().spliterator(), false)
                .forEach(d -> {
                    ForwardingObjective.Flag flag = checkGatewayNode(d.id()) ?
                            ForwardingObjective.Flag.VERSATILE :
                            ForwardingObjective.Flag.SPECIFIC;
                    removeRule(d.id(), sBuilder, flag, ROUTING_RULE_PRIORITY);
                });

    }

    private void removeRule(DeviceId id, TrafficSelector.Builder sBuilder,
                            ForwardingObjective.Flag flag, int priority) {
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withFlag(flag)
                .withPriority(priority)
                .fromApp(appId)
                .remove();

        flowObjectiveService.forward(id, fo);
    }

    /**
     * Populates flow rules for floatingIp configuration.
     *
     * @param floatingIP Corresponding floating ip information
     */
    public void populateFloatingIpRules(OpenstackFloatingIP floatingIP) {
        OpenstackPort port = openstackService.port(floatingIP.portId());
        //1. incoming rules
        populateFloatingIpIncomingRules(floatingIP, port);
        //2. outgoing rules
        populateFloatingIpOutgoingRules(floatingIP, port);
    }

    private void populateFloatingIpIncomingRules(OpenstackFloatingIP floatingIP, OpenstackPort port) {
        DeviceId portDeviceId = getDevicefromOpenstackPort(port).id();
        Device gatewayNode = getGatewayNode();
        Device portNode = deviceService.getDevice(portDeviceId);

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IpPrefix.valueOf(floatingIP.floatingIpAddress(), PREFIX_LENGTH));

        tBuilder.setEthSrc(GATEWAYMAC)
                .setEthDst(port.macAddress())
                .setIpDst(floatingIP.fixedIpAddress())
                .setTunnelId(getVni(port.networkId()))
                .extension(buildNiciraExtenstion(gatewayNode.id(),
                        config.nodes().get(portNode.id())), gatewayNode.id())
                .setOutput(getTunnelPort(gatewayNode.id()));

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(FLOATING_RULE_PRIORITY)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(getGatewayNode().id(), fo);

    }

    private void populateFloatingIpOutgoingRules(OpenstackFloatingIP floatingIP, OpenstackPort port) {
        Port outputPort = checkNotNull(getPortOfExternalInterface(), PORTNOTNULL);

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(getVni(port.networkId()))
                .matchIPSrc(IpPrefix.valueOf(floatingIP.fixedIpAddress(), 32));

        tBuilder.setIpSrc(floatingIP.floatingIpAddress())
                .setEthSrc(MacAddress.valueOf(config.gatewayExternalInterfaceMac()))
                .setEthDst(MacAddress.valueOf(config.physicalRouterMac()))
                .setOutput(outputPort.number());

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(FLOATING_RULE_PRIORITY)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(getGatewayNode().id(), fo);
    }

    /**
     * Removes flow rules for floating ip configuration.
     *
     * @param floatingIP Corresponding floating ip information
     * @param portInfo stored information about deleted vm
     */
    public void removeFloatingIpRules(OpenstackFloatingIP floatingIP, OpenstackPortInfo portInfo) {
        TrafficSelector.Builder sOutgoingBuilder = DefaultTrafficSelector.builder();
        TrafficSelector.Builder sIncomingBuilder = DefaultTrafficSelector.builder();

        sOutgoingBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(portInfo.vni())
                .matchIPSrc(IpPrefix.valueOf(portInfo.ip(), PREFIX_LENGTH));

        sIncomingBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IpPrefix.valueOf(floatingIP.floatingIpAddress(), PREFIX_LENGTH));

        removeRule(getGatewayNode().id(), sOutgoingBuilder, ForwardingObjective.Flag.VERSATILE, FLOATING_RULE_PRIORITY);
        removeRule(getGatewayNode().id(), sIncomingBuilder, ForwardingObjective.Flag.VERSATILE, FLOATING_RULE_PRIORITY);
    }

    /**
     * Populates L3 rules for east to west traffic.
     *
     * @param p target VM
     * @param targetList target openstackRouterInterfaces
     */
    public void populateL3Rules(OpenstackPort p, List<OpenstackRouterInterface> targetList) {
        Device device = getDevicefromOpenstackPort(p);
        Port port = getPortFromOpenstackPort(device, p);
        Ip4Address vmIp = (Ip4Address) p.fixedIps().values().iterator().next();

        if (port == null) {
            return;
        }

        targetList.forEach(routerInterface -> {
            OpenstackPort openstackPort = openstackService.port(routerInterface.portId());
            long vni = getVni(openstackPort.networkId());

            if (vmIp == null) {
                return;
            }

            populateL3RulestoSameNode(vmIp, p, port, device, vni);

            deviceService.getAvailableDevices().forEach(d -> {
                if (!d.equals(device) && !d.equals(getGatewayNode())) {
                    populateL3RulestoDifferentNode(vmIp, vni, d.id(), getHostIpfromOpenstackPort(p));
                }
            });

        });
    }

    private void populateL3RulestoDifferentNode(Ip4Address vmIp, long vni, DeviceId deviceId, Ip4Address hostIp) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(vni)
                .matchIPDst(vmIp.toIpPrefix());
        tBuilder.extension(buildNiciraExtenstion(deviceId, hostIp), deviceId)
                .setOutput(getTunnelPort(deviceId));

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(ROUTING_RULE_PRIORITY)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(deviceId, fo);
    }

    private void populateL3RulestoSameNode(Ip4Address vmIp, OpenstackPort p, Port port, Device device, long vni) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(vmIp.toIpPrefix())
                .matchTunnelId(vni);

        tBuilder.setEthDst(p.macAddress())
                .setOutput(port.number());

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(ROUTING_RULE_PRIORITY)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(device.id(), fo);
    }

    private Port getPortFromOpenstackPort(Device device, OpenstackPort p) {
        String openstackPortName = PORTNAME_PREFIX_VM + p.id().substring(0, 11);
        return  deviceService.getPorts(device.id())
                .stream()
                .filter(pt -> pt.annotations().value(PORTNAME).equals(openstackPortName))
                .findAny()
                .orElse(null);
    }

    /**
     * Removes L3 rules for routerInterface events.
     *
     * @param vmIp Corresponding Vm ip
     * @param routerInterfaces Corresponding routerInterfaces
     */
    public void removeL3Rules(Ip4Address vmIp, List<OpenstackRouterInterface> routerInterfaces) {
        if (vmIp == null) {
            return;
        }

        OpenstackRoutingService routingService = getService(OpenstackRoutingService.class);

        deviceService.getAvailableDevices().forEach(d -> {
            if (!d.equals(getGatewayNode())) {
                routerInterfaces.forEach(routerInterface -> {
                    String networkId = routingService.networkIdForRouterInterface(routerInterface.portId());
                    long vni = getVni(networkId);

                    TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();

                    sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                            .matchIPDst(vmIp.toIpPrefix())
                            .matchTunnelId(vni);

                    removeRule(d.id(), sBuilder, ForwardingObjective.Flag.SPECIFIC, ROUTING_RULE_PRIORITY);
                });
            }
        });
    }
}
