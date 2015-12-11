/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.openstackswitching;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.dhcp.DhcpService;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Port;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

@SuppressWarnings("ALL")
@Service
@Component(immediate = true)
/**
 * Populates forwarding rules for VMs created by Openstack.
 */
public class OpenstackSwitchingManager implements OpenstackSwitchingService {

    private static Logger log = LoggerFactory
            .getLogger(OpenstackSwitchingManager.class);

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
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    protected FlowRuleService flowRuleService;

    private ApplicationId appId;
    private boolean doNotPushFlows;
    private Ip4Address neutronServer;
    private Ip4Address keystoneServer;
    private String userName;
    private String password;
    private OpenstackArpHandler arpHandler;
    private OpenstackRestHandler restHandler;

    private ExecutorService deviceEventExcutorService = Executors.newFixedThreadPool(10);

    private InternalPacketProcessor internalPacketProcessor = new InternalPacketProcessor();
    private InternalDeviceListener internalDeviceListener = new InternalDeviceListener();
    private InternalConfigListener internalConfigListener = new InternalConfigListener();
    private InternalHostListener internalHostListener = new InternalHostListener();
    private final Set<ConfigFactory> factories = ImmutableSet.of(
            new ConfigFactory<ApplicationId, OpenstackSwitchingConfig>(APP_SUBJECT_FACTORY,
                    OpenstackSwitchingConfig.class,
                    "openstackswitching") {
                @Override
                public OpenstackSwitchingConfig createConfig() {
                    return new OpenstackSwitchingConfig();
                }
            }
    );

    @Activate
    protected void activate() {
        appId = coreService
                .registerApplication("org.onosproject.openstackswitching");

        factories.forEach(cfgService::registerConfigFactory);
        packetService.addProcessor(internalPacketProcessor, PacketProcessor.director(1));
        deviceService.addListener(internalDeviceListener);
        hostService.addListener(internalHostListener);
        cfgService.addListener(internalConfigListener);

        internalConfigListener.configureNetwork();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(internalPacketProcessor);
        deviceService.removeListener(internalDeviceListener);
        cfgService.removeListener(internalConfigListener);
        factories.forEach(cfgService::unregisterConfigFactory);

        deviceEventExcutorService.shutdown();

        log.info("Stopped");
    }

    @Override
    public void createPorts(OpenstackPort openstackPort) {
        registerDhcpInfo(openstackPort);
    }

    @Override
    public void deletePort(String uuid) {

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
    public Collection<OpenstackPort> ports(String networkId) {
        Collection<OpenstackPort> ports = restHandler.getPorts();
        List<OpenstackPort> portList = ports.stream()
                .filter(p -> p.networkId().equals(networkId))
                .collect(Collectors.toList());

        return portList;
    }

    @Override
    public OpenstackPort port(Port port) {
        Collection<OpenstackPort> ports = restHandler.getPorts();
        String uuid = port.annotations().value("portName").substring(3);
        return ports.stream()
                .filter(p -> p.id().startsWith(uuid))
                .findFirst().orElse(null);
    }

    @Override
    public OpenstackPort port(String portId) {
        Collection<OpenstackPort> ports = restHandler.getPorts();
        return ports.stream()
                .filter(p -> p.id().equals(portId))
                .findFirst().orElse(null);
    }

    @Override
    public OpenstackNetwork network(String networkId) {
        try {
            Collection<OpenstackSubnet> subnets = restHandler.getSubnets().stream()
                    .filter(s -> s.networkId().equals(networkId))
                    .collect(Collectors.toList());

            Collection<OpenstackNetwork> networks = restHandler.getNetworks();
            OpenstackNetwork openstackNetwork = networks.stream()
                    .filter(n -> n.id().equals(networkId))
                    .findFirst().get();

            return OpenstackNetwork.builder()
                    .id(openstackNetwork.id())
                    .name(openstackNetwork.name())
                    .networkType(openstackNetwork.networkType())
                    .segmentId(openstackNetwork.segmentId())
                    .tenantId(openstackNetwork.tenantId())
                    .subnets(subnets)
                    .build();
        } catch (NoSuchElementException e) {
            log.warn("There is no network infor for net ID {}", networkId);
            return null;
        }
    }

    @Override
    public OpenstackSubnet subnet(String subnetId) {
        Collection<OpenstackSubnet> subnets = restHandler.getSubnets();
        try {
            return subnets.stream()
                    .filter(s -> s.id().equals(subnetId))
                    .findFirst().get();
        } catch (NoSuchElementException e) {
            log.warn("There is no subnet info for subnet ID {}", subnetId);
            return null;
        }
    }

    private void processDeviceAdded(Device device) {
        log.debug("device {} is added", device.id());
    }

    private void processPortAdded(Device device, Port port) {
        if (!port.annotations().value("portName").equals("vxlan")
            && port.isEnabled() && !doNotPushFlows) {
            OpenstackSwitchingRulePopulator rulePopulator =
                    new OpenstackSwitchingRulePopulator(appId, flowObjectiveService,
                            deviceService, restHandler, driverService);
            rulePopulator.populateSwitchingRules(device, port);
        }
    }

    private void processPortRemoved(Device device, Port port) {
        log.debug("port {} is removed", port.toString());
    }

    private void initializeFlowRules() {
        OpenstackSwitchingRulePopulator rulePopulator =
                new OpenstackSwitchingRulePopulator(appId, flowObjectiveService,
                        deviceService, restHandler, driverService);

        deviceService.getDevices().forEach(device -> {
                    log.debug("device {} num of ports {} ", device.id(),
                            deviceService.getPorts(device.id()).size());
                    deviceService.getPorts(device.id()).stream()
                            .filter(port -> port.annotations().value("portName").startsWith("tap"))
                            .forEach(vmPort -> {
                                        OpenstackPort osPort = rulePopulator.openstackPort(vmPort);
                                        if (osPort != null) {
                                            if (!doNotPushFlows) {
                                                rulePopulator.populateSwitchingRules(device, vmPort);
                                            }
                                            registerDhcpInfo(osPort);
                                        } else {
                                            log.warn("No openstackPort information for port {}", vmPort);
                                        }
                                    }
                            );
                }
        );
    }

    private void processHostRemoved(Host host) {
        log.debug("host {} was removed", host.toString());

        try {
            if (!doNotPushFlows) {
                IpAddress hostIp = host.ipAddresses().stream().
                        filter(ip -> ip.isIp4()).findAny().orElse(null);
                OpenstackSwitchingRulePopulator rulePopulator =
                        new OpenstackSwitchingRulePopulator(appId, flowObjectiveService,
                                deviceService, restHandler, driverService);
                rulePopulator.removeSwitchingRules(host.location().deviceId(),
                        hostIp.getIp4Address());
            }

            dhcpService.removeStaticMapping(host.mac());
        } catch (NoSuchElementException e) {
            log.error("No IP address is assigned.");
        }
    }

    private long getVniFromFlowRules(DeviceId deviceId, Ip4Address hostIp) {

        for (FlowEntry flowEntry: flowRuleService.getFlowEntries(deviceId)) {
            Criterion c = flowEntry.selector().getCriterion(Criterion.Type.IPV4_DST);
            if (c != null) {
                IPCriterion destIpCriterion = (IPCriterion) c;
                if (destIpCriterion.ip().getIp4Prefix().address().equals(hostIp)) {
                    for (Instruction i : flowEntry.treatment().immediate()) {
                        if (i.type().equals(Instruction.Type.L2MODIFICATION)) {
                            L2ModificationInstruction l2m = (L2ModificationInstruction) i;
                            if (l2m.subtype().equals(L2ModificationInstruction.L2SubType.TUNNEL_ID)) {
                                L2ModificationInstruction.ModTunnelIdInstruction setTunnelInstr =
                                        (L2ModificationInstruction.ModTunnelIdInstruction) l2m;
                                return setTunnelInstr.tunnelId();
                            }
                        }
                    }
                }
            }
        }

        return 0;
    }

    private void registerDhcpInfo(OpenstackPort openstackPort) {
        Ip4Address ip4Address;
        Ip4Address subnetMask;
        Ip4Address dhcpServer;
        Ip4Address gatewayIPAddress;
        Ip4Address domainServer;
        OpenstackSubnet openstackSubnet;

        ip4Address = (Ip4Address) openstackPort.fixedIps().values().toArray()[0];

        openstackSubnet = restHandler.getSubnets().stream()
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

            if (ethernet.getEtherType() == Ethernet.TYPE_ARP) {
                arpHandler.processPacketIn(pkt);
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
                    case DEVICE_UPDATED:
                        Port port = (Port) deviceEvent.subject();
                        if (port.isEnabled()) {
                            processPortAdded((Device) deviceEvent.subject(), deviceEvent.port());
                        }
                        break;
                    case DEVICE_AVAILABILITY_CHANGED:
                        Device device = (Device) deviceEvent.subject();
                        if (deviceService.isAvailable(device.id())) {
                            processDeviceAdded(device);
                        }
                        break;
                    case PORT_ADDED:
                        processPortAdded((Device) deviceEvent.subject(), deviceEvent.port());
                        break;
                    case PORT_UPDATED:
                        processPortAdded((Device) deviceEvent.subject(), deviceEvent.port());
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

    private class InternalConfigListener implements NetworkConfigListener {

        public void configureNetwork() {
            OpenstackSwitchingConfig cfg =
                    cfgService.getConfig(appId, OpenstackSwitchingConfig.class);
            if (cfg == null) {
                log.error("There is no openstack server information in config.");
                return;
            }
            doNotPushFlows = cfg.doNotPushFlows();
            restHandler = new OpenstackRestHandler(cfg);
            arpHandler = new OpenstackArpHandler(restHandler, packetService);
            initializeFlowRules();
        }

        @Override
        public void event(NetworkConfigEvent event) {
            if (((event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
            event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED)) &&
                    event.configClass().equals(OpenstackSwitchingConfig.class)) {
               configureNetwork();
            }
        }

    }

    private final class PortInfo {
        DeviceId deviceId;
        String portName;
        Ip4Address fixedIp;
        Ip4Address hostIp;

        private PortInfo(DeviceId deviceId, String portName, Ip4Address fixedIp,
                         Ip4Address hostIp) {
            this.deviceId = deviceId;
            this.portName = portName;
            this.fixedIp = fixedIp;
            this.hostIp = hostIp;
        }
    }
}