/*
 * Copyright 2015-2016 Open Networking Laboratory
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
package org.onosproject.openstacknetworking.switching;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcp.DhcpService;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackNetwork;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.openstackinterface.OpenstackSecurityGroup;
import org.onosproject.openstackinterface.OpenstackSubnet;
import org.onosproject.openstacknetworking.OpenstackPortInfo;
import org.onosproject.openstacknetworking.OpenstackSwitchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;

@Service
@Component(immediate = true)
/**
 * Populates forwarding rules for VMs created by Openstack.
 */
public class OpenstackSwitchingManager implements OpenstackSwitchingService {

    private final Logger log = LoggerFactory
            .getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DhcpService dhcpService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    protected OpenstackInterfaceService openstackService;

    public static final String PORTNAME_PREFIX_VM = "tap";
    public static final String PORTNAME_PREFIX_ROUTER = "qr-";
    public static final String PORTNAME_PREFIX_TUNNEL = "vxlan";
    public static final String PORTNAME = "portName";
    private static final String ROUTER_INTERFACE = "network:router_interface";
    public static final String DEVICE_OWNER_GATEWAY = "network:router_gateway";

    private ApplicationId appId;
    private OpenstackArpHandler arpHandler = new OpenstackArpHandler(openstackService, packetService, hostService);

    private ExecutorService deviceEventExcutorService =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackswitching", "device-event"));

    private InternalPacketProcessor internalPacketProcessor = new InternalPacketProcessor();
    private InternalDeviceListener internalDeviceListener = new InternalDeviceListener();
    private InternalHostListener internalHostListener = new InternalHostListener();

    private Map<String, OpenstackPortInfo> openstackPortInfoMap = Maps.newHashMap();

    @Activate
    protected void activate() {
        appId = coreService
                .registerApplication("org.onosproject.openstackswitching");

        packetService.addProcessor(internalPacketProcessor, PacketProcessor.director(1));
        deviceService.addListener(internalDeviceListener);
        hostService.addListener(internalHostListener);

        initializeFlowRules();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(internalPacketProcessor);
        deviceService.removeListener(internalDeviceListener);

        deviceEventExcutorService.shutdown();

        log.info("Stopped");
    }

    @Override
    public void createPorts(OpenstackPort openstackPort) {

        if (!openstackPort.deviceOwner().equals(ROUTER_INTERFACE)
            && !openstackPort.deviceOwner().equals(DEVICE_OWNER_GATEWAY)) {
            if (!openstackPort.fixedIps().isEmpty()) {
                registerDhcpInfo(openstackPort);
            }
        }

        if (!openstackPort.securityGroups().isEmpty()) {
            openstackPort.securityGroups().forEach(sgId -> {
                OpenstackSecurityGroup sg = openstackService.getSecurityGroup(sgId);
                log.debug("SecurityGroup : {}", sg.toString());
            });
        }
    }

    @Override
    public void removePort(String uuid) {
        // When VMs are remvoed, the flow rules for the VMs are removed using ONOS port update event.
        // But, when router is removed, no ONOS port event occurs and we need to use Neutron port event.
        // Here we should not touch any rules for VMs.
        log.debug("port {} was removed", uuid);

        String routerPortName = PORTNAME_PREFIX_ROUTER + uuid.substring(0, 11);
        OpenstackPortInfo routerPortInfo = openstackPortInfoMap.get(routerPortName);
        if (routerPortInfo != null) {
            dhcpService.removeStaticMapping(routerPortInfo.mac());
            deviceService.getPorts(routerPortInfo.deviceId()).forEach(port -> {
                String pName = port.annotations().value("portName");
                if (pName.equals(routerPortName)) {
                    OpenstackSwitchingRulePopulator rulePopulator =
                            new OpenstackSwitchingRulePopulator(appId, flowObjectiveService,
                                    deviceService, openstackService, driverService);

                    rulePopulator.removeSwitchingRules(port, openstackPortInfoMap);
                    openstackPortInfoMap.remove(routerPortName);
                    return;
                }
            });
        }
    }

    @Override
    public void updatePort(OpenstackPort openstackPort) {
    }

    @Override
    public void createNetwork(OpenstackNetwork openstackNetwork) {
    }

    @Override
    public void createSubnet(OpenstackSubnet openstackSubnet) {
    }

    @Override
    public Map<String, OpenstackPortInfo> openstackPortInfo() {
        return ImmutableMap.copyOf(this.openstackPortInfoMap);
    }

    private void processDeviceAdded(Device device) {
        log.debug("device {} is added", device.id());
    }

    private void processPortUpdated(Device device, Port port) {
        if (!port.annotations().value(PORTNAME).equals(PORTNAME_PREFIX_TUNNEL)) {
            if (port.isEnabled() || port.annotations().value(PORTNAME).startsWith(PORTNAME_PREFIX_ROUTER)) {
                OpenstackSwitchingRulePopulator rulePopulator =
                        new OpenstackSwitchingRulePopulator(appId, flowObjectiveService,
                                deviceService, openstackService, driverService);

                rulePopulator.populateSwitchingRules(device, port);
                updatePortMap(device.id(), port, openstackService.networks(), openstackService.subnets(),
                        rulePopulator.openstackPort(port));

                //In case portupdate event is driven by vm shutoff from openstack
            } else if (!port.isEnabled() && openstackPortInfoMap.containsKey(port.annotations().value(PORTNAME))) {
                log.debug("Flowrules according to the port {} were removed", port.number().toString());
                OpenstackSwitchingRulePopulator rulePopulator =
                        new OpenstackSwitchingRulePopulator(appId, flowObjectiveService,
                                deviceService, openstackService, driverService);

                rulePopulator.removeSwitchingRules(port, openstackPortInfoMap);
                dhcpService.removeStaticMapping(openstackPortInfoMap.get(port.annotations().value(PORTNAME)).mac());
                openstackPortInfoMap.remove(port.annotations().value(PORTNAME));
            }
        }
    }

    private void processPortRemoved(Device device, Port port) {
        log.debug("port {} is removed", port.toString());
    }

    private void initializeFlowRules() {
        OpenstackSwitchingRulePopulator rulePopulator =
                new OpenstackSwitchingRulePopulator(appId, flowObjectiveService,
                        deviceService, openstackService, driverService);

        Collection<OpenstackNetwork> networks = openstackService.networks();
        Collection<OpenstackSubnet> subnets = openstackService.subnets();

        deviceService.getDevices().forEach(device -> {
                    log.debug("device {} num of ports {} ", device.id(),
                            deviceService.getPorts(device.id()).size());
                    deviceService.getPorts(device.id()).stream()
                            .filter(port -> port.annotations().value(PORTNAME).startsWith(PORTNAME_PREFIX_VM) ||
                                    port.annotations().value(PORTNAME).startsWith(PORTNAME_PREFIX_ROUTER))
                            .forEach(vmPort -> {
                                        OpenstackPort osPort = rulePopulator.openstackPort(vmPort);
                                        if (osPort != null && !osPort.deviceOwner().equals(DEVICE_OWNER_GATEWAY)) {
                                            rulePopulator.populateSwitchingRules(device, vmPort);
                                            updatePortMap(device.id(), vmPort, networks, subnets, osPort);
                                            registerDhcpInfo(osPort);
                                        } else {
                                            log.warn("No openstackPort information for port {}", vmPort);
                                        }
                                    }
                            );
                }
        );
    }

    private void updatePortMap(DeviceId deviceId, Port port, Collection<OpenstackNetwork> networks,
                               Collection<OpenstackSubnet> subnets, OpenstackPort openstackPort) {
        long vni = Long.parseLong(networks.stream()
                .filter(n -> n.id().equals(openstackPort.networkId()))
                .findAny().orElse(null).segmentId());

        OpenstackSubnet openstackSubnet = subnets.stream()
                .filter(n -> n.networkId().equals(openstackPort.networkId()))
                .findFirst().get();

        Ip4Address gatewayIPAddress = Ip4Address.valueOf(openstackSubnet.gatewayIp());

        OpenstackPortInfo.Builder portBuilder = OpenstackPortInfo.builder()
                .setDeviceId(deviceId)
                .setHostIp((Ip4Address) openstackPort.fixedIps().values().stream().findFirst().orElse(null))
                .setHostMac(openstackPort.macAddress())
                .setVni(vni)
                .setGatewayIP(gatewayIPAddress);

        openstackPortInfoMap.putIfAbsent(port.annotations().value(PORTNAME),
                portBuilder.build());
    }

    private void processHostRemoved(Host host) {
        log.debug("host {} was removed", host.toString());
    }

    private void registerDhcpInfo(OpenstackPort openstackPort) {
        Ip4Address ip4Address;
        Ip4Address subnetMask;
        Ip4Address gatewayIPAddress;
        Ip4Address dhcpServer;
        Ip4Address domainServer;
        OpenstackSubnet openstackSubnet;

        ip4Address = (Ip4Address) openstackPort.fixedIps().values().stream().findFirst().orElse(null);

        openstackSubnet = openstackService.subnets().stream()
                .filter(n -> n.networkId().equals(openstackPort.networkId()))
                .findFirst().get();

        subnetMask = Ip4Address.valueOf(buildSubnetMask(openstackSubnet.cidr()));
        gatewayIPAddress = Ip4Address.valueOf(openstackSubnet.gatewayIp());
        dhcpServer = gatewayIPAddress;
        // TODO: supports multiple DNS servers
        if (openstackSubnet.dnsNameservers().isEmpty()) {
            domainServer = Ip4Address.valueOf("8.8.8.8");
        } else {
            domainServer = openstackSubnet.dnsNameservers().get(0);
        }
        List<Ip4Address> options = Lists.newArrayList();
        options.add(subnetMask);
        options.add(dhcpServer);
        options.add(gatewayIPAddress);
        options.add(domainServer);

        dhcpService.setStaticMapping(openstackPort.macAddress(), ip4Address, true, options);
    }

    private byte[] buildSubnetMask(String cidr) {
        int prefix;
        String[] parts = cidr.split("/");
        prefix = Integer.parseInt(parts[1]);
        int mask = 0xffffffff << (32 - prefix);
        byte[] bytes = new byte[]{(byte) (mask >>> 24),
                (byte) (mask >> 16 & 0xff), (byte) (mask >> 8 & 0xff), (byte) (mask & 0xff)};

        return bytes;
    }



    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethernet = pkt.parsed();

            if (ethernet != null && ethernet.getEtherType() == Ethernet.TYPE_ARP) {
                arpHandler.processPacketIn(pkt, openstackPortInfoMap.values());
            }
        }
    }

    private class InternalHostListener implements HostListener {

        @Override
        public void event(HostEvent hostEvent) {
            deviceEventExcutorService.execute(new InternalEventHandler(hostEvent));
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent deviceEvent) {
            deviceEventExcutorService.execute(new InternalEventHandler(deviceEvent));
        }
    }

    private class InternalEventHandler implements Runnable {

        volatile AbstractEvent event;

        InternalEventHandler(AbstractEvent event) {
            this.event = event;
        }

        @Override
        public void run() {

            if (event instanceof  DeviceEvent) {
                DeviceEvent deviceEvent = (DeviceEvent) event;

                switch (deviceEvent.type()) {
                    case DEVICE_ADDED:
                        processDeviceAdded((Device) deviceEvent.subject());
                        break;
                    case DEVICE_AVAILABILITY_CHANGED:
                        Device device = (Device) deviceEvent.subject();
                        if (deviceService.isAvailable(device.id())) {
                            processDeviceAdded(device);
                        }
                        break;
                    case PORT_ADDED:
                        processPortUpdated((Device) deviceEvent.subject(), deviceEvent.port());
                        break;
                    case PORT_UPDATED:
                        processPortUpdated((Device) deviceEvent.subject(), deviceEvent.port());
                        break;
                    case PORT_REMOVED:
                        processPortRemoved((Device) deviceEvent.subject(), deviceEvent.port());
                        break;
                    default:
                        break;
                }
            } else if (event instanceof HostEvent) {
                HostEvent hostEvent = (HostEvent) event;

                switch (hostEvent.type()) {
                    case HOST_REMOVED:
                        processHostRemoved((Host) hostEvent.subject());
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
