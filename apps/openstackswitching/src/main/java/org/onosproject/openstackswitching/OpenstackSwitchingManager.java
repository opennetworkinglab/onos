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
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.UDP;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("ALL")
@Service
@Component(immediate = true)
/**
 * It populates forwarding rules for VMs created by Openstack.
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
    protected FlowObjectiveService flowObjectiveService;


    public static final int DHCP_PORT = 67;

    private ApplicationId appId;
    private OpenstackArpHandler arpHandler;
    private OpenstackDhcpHandler dhcpHandler = new OpenstackDhcpHandler();
    private OpenstackSwitchingRulePopulator rulePopulator;
    private ExecutorService deviceEventExcutorService = Executors.newFixedThreadPool(10);

    private InternalPacketProcessor internalPacketProcessor = new InternalPacketProcessor();
    private InternalDeviceListener internalDeviceListener = new InternalDeviceListener();

    // Map <port_id, OpenstackPort>
    private HashMap<String, OpenstackPort> openstackPortMap;
    // Map <network_id, OpenstackNetwork>
    private HashMap<String, OpenstackNetwork> openstackNetworkMap;
    // Map <vni, List <Entry <portName, host ip>>
    private HashMap<String, List<PortInfo>> vniPortMap;
    private HashMap<Ip4Address, Port> tunnelPortMap;


    @Activate
    protected void activate() {
        appId = coreService
                .registerApplication("org.onosproject.openstackswitching");
        rulePopulator = new OpenstackSwitchingRulePopulator(appId, flowObjectiveService);
        packetService.addProcessor(internalPacketProcessor, PacketProcessor.director(1));
        deviceService.addListener(internalDeviceListener);

        openstackPortMap = Maps.newHashMap();
        openstackNetworkMap = Maps.newHashMap();
        vniPortMap = Maps.newHashMap();
        tunnelPortMap = Maps.newHashMap();

        arpHandler = new OpenstackArpHandler(openstackPortMap);

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
        openstackPortMap.put(openstackPort.id(), openstackPort);
    }

    @Override
    public void deletePorts() {

    }

    @Override
    public void updatePorts() {

    }

    @Override
    public void createNetwork(OpenstackNetwork openstackNetwork) {
        openstackNetworkMap.put(openstackNetwork.id(), openstackNetwork);
    }

    private void processDeviceAdded(Device device) {
        log.warn("device {} is added", device.id());
        rulePopulator.populateDefaultRules(device.id());
    }

    private void processPortAdded(Device device, Port port) {
        // TODO: Simplify the data structure to store the network info
        // TODO: Make it stateless
        // TODO: All the logics need to be processed inside of the rulePopulator class
        synchronized (vniPortMap) {
            log.warn("port {} is updated", port.toString());

            updatePortMaps(device, port);
            if (!port.annotations().value("portName").equals("vxlan")) {
                populateFlowRulesForTrafficToSameCnode(device, port);
                populateFlowRulesForTrafficToDifferentCnode(device, port);
            }
        }
    }

    private void processPortRemoved(Device device, Port port) {
        log.warn("port {} is removed", port.toString());
        // TODO: need to update the vniPortMap
    }

    /**
     * Populates the flow rules for traffic to VMs in different Cnode using
     * Nicira extention.
     *
     * @param device device to put rules
     * @param port port information of the VM
     */
    private void populateFlowRulesForTrafficToDifferentCnode(Device device, Port port) {
        String portName = port.annotations().value("portName");
        String channelId = device.annotations().value("channelId");
        Ip4Address hostIpAddress = Ip4Address.valueOf(channelId.split(":")[0]);
        Ip4Address fixedIp = getFixedIpAddressForPort(portName);
        // TODO: Avoid duplicate flow rule set up for VMs in other Cnode
        //       (possibly avoided by flowrule subsystem?)
        if (tunnelPortMap.get(hostIpAddress) == null) {
            log.warn("There is no tunnel port information");
            return;
        }
        String vni = getVniForPort(portName);
        MacAddress vmMac = getVmMacAddressForPort(portName);
        if (!vniPortMap.isEmpty() && vniPortMap.get(vni) != null) {
            for (PortInfo portInfo : vniPortMap.get(vni)) {
                if (!portInfo.portName.equals(portName) &&
                        !portInfo.hostIp.equals(hostIpAddress)) {
                    MacAddress vmMacx = getVmMacAddressForPort(portInfo.portName);
                    rulePopulator.populateForwardingRuleForOtherCnode(vni,
                            device.id(), portInfo.hostIp, portInfo.fixedIp, vmMacx,
                            tunnelPortMap.get(hostIpAddress).number(),
                            portInfo.deviceId, hostIpAddress, fixedIp, vmMac,
                            tunnelPortMap.get(portInfo.hostIp).number());
                }
            }
        }
    }

    /**
     * Populates the flow rules for traffic to VMs in the same Cnode as the sender.
     *
     * @param device device to put the rules
     * @param port port info of the VM
     */
    private void populateFlowRulesForTrafficToSameCnode(Device device, Port port) {
        Ip4Prefix cidr = getCidrForPort(port.annotations().value("portName"));
        Ip4Address vmIp = getFixedIpAddressForPort(port.annotations().value("portName"));
        if (vmIp != null) {
            rulePopulator.populateForwardingRule(vmIp, device.id(), port, cidr);
        }
    }

    /**
     * Updates the port maps using the port information.
     *
     * @param device device info
     * @param port port of the VM
     */
    private void updatePortMaps(Device device, Port port) {
        String portName = port.annotations().value("portName");
        String channelId = device.annotations().value("channelId");
        Ip4Address hostIpAddress = Ip4Address.valueOf(channelId.split(":")[0]);
        if (portName.startsWith("vxlan")) {
            tunnelPortMap.put(hostIpAddress, port);
        } else {
            String vni = getVniForPort(portName);
            Ip4Address fixedIp = getFixedIpAddressForPort(portName);
            if (vniPortMap.get(vni) == null) {
                vniPortMap.put(vni, Lists.newArrayList());
            }
            vniPortMap.get(vni).add(new PortInfo(device.id(), portName, fixedIp, hostIpAddress));
        }
    }

    /**
     * Returns CIDR information from the subnet map for the port.
     *
     * @param portName port name of the port of the VM
     * @return CIDR of the VNI of the VM
     */
    private Ip4Prefix getCidrForPort(String portName) {
        String networkId = null;
        String uuid = portName.substring(3);
        OpenstackPort port = openstackPortMap.values().stream()
                .filter(p -> p.id().startsWith(uuid))
                .findFirst().get();
        if (port == null) {
            log.warn("No port information for port {}", portName);
            return null;
        }

        //OpenstackSubnet subnet = openstackSubnetMap.values().stream()
        //        .filter(s -> s.networkId().equals(port.networkId()))
        //        .findFirst().get();
        //if (subnet == null) {
        //    log.warn("No subnet information for network {}", subnet.id());
        //    return null;
        //}

        //return Ip4Prefix.valueOf(subnet.cidr());
        return null;
    }

    /**
     * Returns the VNI of the VM of the port.
     *
     * @param portName VM port
     * @return VNI
     */
    private String getVniForPort(String portName) {
        String networkId = null;
        String uuid = portName.substring(3);
        OpenstackPort port = openstackPortMap.values().stream()
                .filter(p -> p.id().startsWith(uuid))
                .findFirst().get();
        if (port == null) {
            log.warn("No port information for port {}", portName);
            return null;
        }
        OpenstackNetwork network = openstackNetworkMap.values().stream()
                .filter(n -> n.id().equals(port.networkId()))
                .findFirst().get();
        if (network == null) {
            log.warn("No VNI information for network {}", network.id());
            return null;
        }

        return network.segmentId();
    }

    /**
     * Returns the Fixed IP address of the VM.
     *
     * @param portName VM port info
     * @return IP address of the VM
     */
    private Ip4Address getFixedIpAddressForPort(String portName) {

        // FIXME - For now we use the information stored from neutron Rest API call.
        // TODO - Later, the information needs to be extracted from Neutron on-demand.
        String uuid = portName.substring(3);
        OpenstackPort port = openstackPortMap.values().stream()
                        .filter(p -> p.id().startsWith(uuid))
                        .findFirst().get();

        if (port == null) {
            log.error("There is no port information for port name {}", portName);
            return null;
        }

        if (port.fixedIps().isEmpty()) {
            log.error("There is no fixed IP info in the port information");
            return null;
        }

        return (Ip4Address) port.fixedIps().values().toArray()[0];
    }

    /**
     * Returns the MAC address of the VM of the port.
     *
     * @param portName VM port
     * @return MAC address of the VM
     */
    private MacAddress getVmMacAddressForPort(String portName) {

        String uuid = portName.substring(3);
        OpenstackPort port = openstackPortMap.values().stream()
                .filter(p -> p.id().startsWith(uuid))
                .findFirst().get();

        if (port == null) {
            log.error("There is no mac information for port name {}", portName);
            return null;
        }

        return port.macAddress();
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
            } else if (ethernet.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 ipPacket = (IPv4) ethernet.getPayload();

                if (ipPacket.getProtocol() == IPv4.PROTOCOL_UDP) {
                    UDP udpPacket = (UDP) ipPacket.getPayload();
                    if (udpPacket.getDestinationPort() == DHCP_PORT) {
                        dhcpHandler.processPacketIn(pkt);
                    }
                }
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            deviceEventExcutorService.execute(new InternalEventHandler(event));
        }
    }

    private class InternalEventHandler implements Runnable {

        volatile DeviceEvent deviceEvent;

        InternalEventHandler(DeviceEvent deviceEvent) {
            this.deviceEvent = deviceEvent;
        }

        @Override
        public void run() {
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