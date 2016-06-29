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
package org.onosproject.openstacknetworking.switching;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpPrefix;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcp.DhcpService;
import org.onosproject.dhcp.IpAssignment;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
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
import org.onosproject.openstacknetworking.OpenstackSubjectFactories;
import org.onosproject.openstacknetworking.OpenstackNetworkingConfig;
import org.onosproject.openstacknetworking.OpenstackSwitchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.dhcp.IpAssignment.AssignmentStatus.Option_RangeNotEnforced;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Service
@Component(immediate = true)
/**
 * Populates forwarding rules for VMs created by Openstack.
 */
public class OpenstackSwitchingManager implements OpenstackSwitchingService {

    private final Logger log = LoggerFactory.getLogger(getClass());

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry networkConfig;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackInterfaceService openstackService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    public static final String PORTNAME_PREFIX_VM = "tap";
    public static final String PORTNAME_PREFIX_ROUTER = "qr-";
    public static final String PORTNAME_PREFIX_TUNNEL = "vxlan";
    public static final String PORTNAME = "portName";
    private static final String ROUTER_INTERFACE = "network:router_interface";
    public static final String DEVICE_OWNER_GATEWAY = "network:router_gateway";
    public static final Ip4Address DNS_SERVER_IP = Ip4Address.valueOf("8.8.8.8");
    private static final String FORWARD_SLASH = "/";
    private static final int DHCP_INFINITE_LEASE = -1;
    public static final String SONA_DRIVER_NAME = "sona";


    private ApplicationId appId;

    private OpenstackArpHandler arpHandler;
    private OpenstackSecurityGroupRulePopulator sgRulePopulator;

    private ExecutorService deviceEventExecutorService =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackswitching", "device-event"));

    private ExecutorService configEventExecutorService =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackswitching", "config-event"));

    private InternalPacketProcessor internalPacketProcessor = new InternalPacketProcessor();
    private InternalDeviceListener internalDeviceListener = new InternalDeviceListener();
    private InternalHostListener internalHostListener = new InternalHostListener();

    private final Map<String, OpenstackPortInfo> openstackPortInfoMap = Maps.newHashMap();
    private Map<String, OpenstackSecurityGroup> securityGroupMap = Maps.newConcurrentMap();

    private final ConfigFactory configFactory =
            new ConfigFactory(OpenstackSubjectFactories.USER_DEFINED_SUBJECT_FACTORY, OpenstackNetworkingConfig.class,
                    "config") {
                @Override
                public OpenstackNetworkingConfig createConfig() {
                    return new OpenstackNetworkingConfig();
                }
            };
    private final NetworkConfigListener configListener = new InternalConfigListener();

    private OpenstackNetworkingConfig config;

    @Activate
    protected void activate() {
        appId = coreService
                .registerApplication("org.onosproject.openstackswitching");

        packetService.addProcessor(internalPacketProcessor, PacketProcessor.director(1));
        deviceService.addListener(internalDeviceListener);
        hostService.addListener(internalHostListener);
        configRegistry.registerConfigFactory(configFactory);
        configService.addListener(configListener);

        arpHandler = new OpenstackArpHandler(openstackService, packetService, hostService);
        sgRulePopulator = new OpenstackSecurityGroupRulePopulator(appId, openstackService, flowObjectiveService);

        networkConfig.registerConfigFactory(configFactory);
        networkConfig.addListener(configListener);

        readConfiguration();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(internalPacketProcessor);
        deviceService.removeListener(internalDeviceListener);

        deviceEventExecutorService.shutdown();
        configEventExecutorService.shutdown();
        hostService.removeListener(internalHostListener);
        configService.removeListener(configListener);
        configRegistry.unregisterConfigFactory(configFactory);

        log.info("Stopped");
    }

    @Override
    public void createPorts(OpenstackPort openstackPort) {

        if (!openstackPort.deviceOwner().equals(ROUTER_INTERFACE)
                && !openstackPort.deviceOwner().equals(DEVICE_OWNER_GATEWAY)
                && !openstackPort.fixedIps().isEmpty()) {
                registerDhcpInfo(openstackPort);
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
                String pName = port.annotations().value(PORTNAME);
                if (pName.equals(routerPortName)) {
                    OpenstackSwitchingRulePopulator rulePopulator =
                            new OpenstackSwitchingRulePopulator(appId, flowObjectiveService,
                                    deviceService, openstackService, driverService, config);

                    rulePopulator.removeSwitchingRules(port, openstackPortInfoMap);
                    openstackPortInfoMap.remove(routerPortName);
                    return;
                }
            });
        }
    }

    @Override
    public void updatePort(OpenstackPort openstackPort) {
        if (openstackPort.status().equals(OpenstackPort.PortStatus.ACTIVE)) {
            String portName = PORTNAME_PREFIX_VM + openstackPort.id().substring(0, 11);
            OpenstackPortInfo osPortInfo = openstackPortInfoMap.get(portName);
            if (osPortInfo != null) {
                // Remove all security group rules based on the ones stored in security group map.
                osPortInfo.securityGroups().stream().forEach(
                        sgId -> sgRulePopulator.removeSecurityGroupRules(osPortInfo.deviceId(), sgId,
                                osPortInfo.ip(), openstackPortInfoMap, securityGroupMap));
                // Add all security group rules based on the updated security group.
                openstackPort.securityGroups().stream().forEach(
                        sgId -> sgRulePopulator.populateSecurityGroupRules(osPortInfo.deviceId(), sgId,
                                osPortInfo.ip(), openstackPortInfoMap));
                updatePortMap(osPortInfo.deviceId(), portName, openstackService.networks(),
                        openstackService.subnets(), openstackPort);
            }
        }
    }

    @Override
    public void createNetwork(OpenstackNetwork openstackNetwork) {
        //TODO
    }

    @Override
    public void createSubnet(OpenstackSubnet openstackSubnet) {
        //TODO
    }

    @Override
    public Map<String, OpenstackPortInfo> openstackPortInfo() {
        return ImmutableMap.copyOf(this.openstackPortInfoMap);
    }

    private void processPortUpdated(Device device, Port port) {
        String portName = port.annotations().value(PORTNAME);
        synchronized (openstackPortInfoMap) {
            if (portName.startsWith(PORTNAME_PREFIX_VM)) {
                if (port.isEnabled()) {
                    OpenstackSwitchingRulePopulator rulePopulator =
                            new OpenstackSwitchingRulePopulator(appId, flowObjectiveService,
                                    deviceService, openstackService, driverService, config);

                    rulePopulator.populateSwitchingRules(device, port);
                    OpenstackPort openstackPort = rulePopulator.openstackPort(port);
                    Ip4Address vmIp = (Ip4Address) openstackPort.fixedIps().values().stream()
                            .findAny().orElseGet(null);
                    openstackPort.securityGroups().stream().forEach(
                            sgId -> sgRulePopulator.populateSecurityGroupRules(device.id(), sgId, vmIp,
                                    openstackPortInfoMap));
                    updatePortMap(device.id(), port.annotations().value(PORTNAME),
                            openstackService.networks(), openstackService.subnets(), openstackPort);

                    //In case portupdate event is driven by vm shutoff from openstack
                } else if (!port.isEnabled() && openstackPortInfoMap.containsKey(portName)) {
                    log.debug("Flowrules according to the port {} were removed", port.number());
                    OpenstackSwitchingRulePopulator rulePopulator =
                            new OpenstackSwitchingRulePopulator(appId, flowObjectiveService,
                                    deviceService, openstackService, driverService, config);
                    rulePopulator.removeSwitchingRules(port, openstackPortInfoMap);
                    openstackPortInfoMap.get(portName).securityGroups().stream().forEach(
                            sgId -> sgRulePopulator.removeSecurityGroupRules(device.id(), sgId,
                                    openstackPortInfoMap.get(portName).ip(), openstackPortInfoMap, securityGroupMap));
                    dhcpService.removeStaticMapping(openstackPortInfoMap.get(port.annotations().value(PORTNAME)).mac());
                    openstackPortInfoMap.remove(port.annotations().value(PORTNAME));
                }
            }
        }
    }

    private void processPortRemoved(Port port) {
        log.debug("port {} is removed", port.toString());
    }

    private void initializeFlowRules() {
        OpenstackSwitchingRulePopulator rulePopulator =
                new OpenstackSwitchingRulePopulator(appId, flowObjectiveService,
                        deviceService, openstackService, driverService, config);

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
                                    Ip4Address vmIp = (Ip4Address) osPort.fixedIps().values().stream()
                                            .findAny().orElseGet(null);
                                    osPort.securityGroups().stream().forEach(
                                            sgId -> sgRulePopulator.populateSecurityGroupRules(device.id(),
                                                    sgId, vmIp, openstackPortInfoMap));
                                    updatePortMap(device.id(), vmPort.annotations().value(PORTNAME), networks,
                                            subnets, osPort);
                                    registerDhcpInfo(osPort);
                                } else {
                                    log.warn("No openstackPort information for port {}", vmPort);
                                }
                            }
                        );
                }
        );
    }

    private void updatePortMap(DeviceId deviceId, String portName, Collection<OpenstackNetwork> networks,
                               Collection<OpenstackSubnet> subnets, OpenstackPort openstackPort) {
        long vni;
        OpenstackNetwork openstackNetwork = networks.stream()
                .filter(n -> n.id().equals(openstackPort.networkId()))
                .findAny().orElse(null);
        if (openstackNetwork != null) {
            vni = Long.parseLong(openstackNetwork.segmentId());
        } else {
            log.debug("updatePortMap failed because there's no OpenstackNetwork matches {}", openstackPort.networkId());
            return;
        }


        OpenstackSubnet openstackSubnet = subnets.stream()
                .filter(n -> n.networkId().equals(openstackPort.networkId()))
                .findFirst().get();

        Ip4Address gatewayIPAddress = Ip4Address.valueOf(openstackSubnet.gatewayIp());

        OpenstackPortInfo.Builder portBuilder = OpenstackPortInfo.builder()
                .setDeviceId(deviceId)
                .setHostIp((Ip4Address) openstackPort.fixedIps().values().stream().findFirst().orElse(null))
                .setHostMac(openstackPort.macAddress())
                .setVni(vni)
                .setGatewayIP(gatewayIPAddress)
                .setNetworkId(openstackPort.networkId())
                .setSecurityGroups(openstackPort.securityGroups());

        openstackPortInfoMap.put(portName, portBuilder.build());

        openstackPort.securityGroups().stream().forEach(sgId -> {
            if (!securityGroupMap.containsKey(sgId)) {
                securityGroupMap.put(sgId, openstackService.securityGroup(sgId));
            }
        });
    }

    private void registerDhcpInfo(OpenstackPort openstackPort) {
        checkNotNull(openstackPort);
        checkArgument(!openstackPort.fixedIps().isEmpty());

        OpenstackSubnet openstackSubnet = openstackService.subnets().stream()
                .filter(n -> n.networkId().equals(openstackPort.networkId()))
                .findFirst().orElse(null);
        if (openstackSubnet == null) {
            log.warn("Failed to find subnet for {}", openstackPort);
            return;
        }

        Ip4Address ipAddress = openstackPort.fixedIps().values().stream().findFirst().get();
        IpPrefix subnetPrefix = IpPrefix.valueOf(openstackSubnet.cidr());
        Ip4Address broadcast = Ip4Address.makeMaskedAddress(
                ipAddress,
                subnetPrefix.prefixLength());

        // TODO: supports multiple DNS servers
        Ip4Address domainServer = openstackSubnet.dnsNameservers().isEmpty() ?
                DNS_SERVER_IP : openstackSubnet.dnsNameservers().get(0);

        IpAssignment ipAssignment = IpAssignment.builder()
                .ipAddress(ipAddress)
                .leasePeriod(DHCP_INFINITE_LEASE)
                .timestamp(new Date())
                .subnetMask(Ip4Address.makeMaskPrefix(subnetPrefix.prefixLength()))
                .broadcast(broadcast)
                .domainServer(domainServer)
                .assignmentStatus(Option_RangeNotEnforced)
                .routerAddress(Ip4Address.valueOf(openstackSubnet.gatewayIp()))
                .build();

        dhcpService.setStaticMapping(openstackPort.macAddress(), ipAssignment);
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            // FIXME: use GatewayNode list to check if the ARP packet is from GatewayNode's
            if (context.isHandled()) {
                return;
            } else if (!SONA_DRIVER_NAME.equals(driverService
                    .getDriver(context.inPacket().receivedFrom().deviceId()).name())) {
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
            deviceEventExecutorService.execute(new InternalEventHandler(hostEvent));
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent deviceEvent) {
            deviceEventExecutorService.execute(new InternalEventHandler(deviceEvent));
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
                        log.debug("device {} is added", deviceEvent.subject().id());
                        break;
                    case DEVICE_AVAILABILITY_CHANGED:
                        Device device = deviceEvent.subject();
                        if (deviceService.isAvailable(device.id())) {
                            log.debug("device {} is added", device.id());
                        }
                        break;
                    case PORT_ADDED:
                        processPortUpdated(deviceEvent.subject(), deviceEvent.port());
                        break;
                    case PORT_UPDATED:
                        processPortUpdated(deviceEvent.subject(), deviceEvent.port());
                        break;
                    case PORT_REMOVED:
                        processPortRemoved(deviceEvent.port());
                        break;
                    default:
                        log.debug("Unsupported deviceEvent type {}", deviceEvent.type().toString());
                        break;
                }
            } else if (event instanceof HostEvent) {
                HostEvent hostEvent = (HostEvent) event;

                switch (hostEvent.type()) {
                    case HOST_REMOVED:
                        log.debug("host {} was removed", hostEvent.subject().toString());
                        break;
                    default:
                        log.debug("Unsupported hostEvent type {}", hostEvent.type().toString());
                        break;
                }
            }
        }
    }

    private void readConfiguration() {
        config = configService.getConfig("openstacknetworking", OpenstackNetworkingConfig.class);
        if (config == null) {
            log.error("No configuration found");
            return;
        }

        arpHandler = new OpenstackArpHandler(openstackService, packetService, hostService);
        sgRulePopulator = new OpenstackSecurityGroupRulePopulator(appId, openstackService, flowObjectiveService);

        initializeFlowRules();
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(OpenstackNetworkingConfig.class)) {
                return;
            }

            if (event.type().equals(NetworkConfigEvent.Type.CONFIG_ADDED) ||
                    event.type().equals(NetworkConfigEvent.Type.CONFIG_UPDATED)) {
                configEventExecutorService.execute(OpenstackSwitchingManager.this::readConfiguration);

            }
        }
    }
}
