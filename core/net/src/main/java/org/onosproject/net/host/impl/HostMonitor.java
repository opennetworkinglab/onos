/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.net.host.impl;

import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IPv6;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.ndp.NeighborDiscoveryOptions;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onlab.util.Timer;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.host.PortAddresses;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.ProviderId;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Monitors hosts on the dataplane to detect changes in host data.
 * <p>
 * The HostMonitor can monitor hosts that have already been detected for
 * changes. At an application's request, it can also monitor and actively
 * probe for hosts that have not yet been detected (specified by IP address).
 * </p>
 */
public class HostMonitor implements TimerTask {
    private DeviceService deviceService;
    private PacketService packetService;
    private HostManager hostManager;

    private final Set<IpAddress> monitoredAddresses;

    private final ConcurrentMap<ProviderId, HostProvider> hostProviders;

    private static final long DEFAULT_PROBE_RATE = 30000; // milliseconds
    private static final byte[] ZERO_MAC_ADDRESS = MacAddress.ZERO.toBytes();
    private long probeRate = DEFAULT_PROBE_RATE;

    private Timeout timeout;

    /**
     * Creates a new host monitor.
     *
     * @param deviceService device service used to find edge ports
     * @param packetService packet service used to send packets on the data plane
     * @param hostManager host manager used to look up host information and
     * probe existing hosts
     */
    public HostMonitor(DeviceService deviceService, PacketService packetService,
            HostManager hostManager) {

        this.deviceService = deviceService;
        this.packetService = packetService;
        this.hostManager = hostManager;

        monitoredAddresses = Collections.newSetFromMap(
                new ConcurrentHashMap<IpAddress, Boolean>());
        hostProviders = new ConcurrentHashMap<>();
    }

    /**
     * Adds an IP address to be monitored by the host monitor. The monitor will
     * periodically probe the host to detect changes.
     *
     * @param ip IP address of the host to monitor
     */
    void addMonitoringFor(IpAddress ip) {
        monitoredAddresses.add(ip);
    }

    /**
     * Stops monitoring the given IP address.
     *
     * @param ip IP address to stop monitoring on
     */
    void stopMonitoring(IpAddress ip) {
        monitoredAddresses.remove(ip);
    }

    /**
     * Starts the host monitor. Does nothing if the monitor is already running.
     */
    void start() {
        synchronized (this) {
            if (timeout == null) {
                timeout = Timer.getTimer().newTimeout(this, 0, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Stops the host monitor.
     */
    void shutdown() {
        synchronized (this) {
            timeout.cancel();
            timeout = null;
        }
    }

    /**
     * Registers a host provider with the host monitor. The monitor can use the
     * provider to probe hosts.
     *
     * @param provider the host provider to register
     */
    void registerHostProvider(HostProvider provider) {
        hostProviders.put(provider.id(), provider);
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        for (IpAddress ip : monitoredAddresses) {
            Set<Host> hosts = hostManager.getHostsByIp(ip);

            if (hosts.isEmpty()) {
                sendArpNdpRequest(ip);
            } else {
                for (Host host : hosts) {
                    HostProvider provider = hostProviders.get(host.providerId());
                    if (provider == null) {
                        hostProviders.remove(host.providerId(), null);
                    } else {
                        provider.triggerProbe(host);
                    }
                }
            }
        }

        this.timeout = Timer.getTimer().newTimeout(this, probeRate, TimeUnit.MILLISECONDS);
    }

    /**
     * Sends an ARP or Neighbor Discovery Protocol request for the given IP
     * address.
     *
     * @param targetIp IP address to send the request for
     */
    private void sendArpNdpRequest(IpAddress targetIp) {
        // Find ports with an IP address in the target's subnet and sent ARP/ND
        // probes out those ports.
        for (Device device : deviceService.getDevices()) {
            for (Port port : deviceService.getPorts(device.id())) {
                ConnectPoint cp = new ConnectPoint(device.id(), port.number());
                Set<PortAddresses> portAddressSet =
                    hostManager.getAddressBindingsForPort(cp);

                for (PortAddresses portAddresses : portAddressSet) {
                    for (InterfaceIpAddress ia : portAddresses.ipAddresses()) {
                        if (ia.subnetAddress().contains(targetIp)) {
                            sendArpNdpProbe(device.id(), port, targetIp,
                                            ia.ipAddress(),
                                            portAddresses.mac(),
                                            portAddresses.vlan());
                        }
                    }
                }
            }
        }
    }

    private void sendArpNdpProbe(DeviceId deviceId, Port port,
                                 IpAddress targetIp,
                                 IpAddress sourceIp, MacAddress sourceMac,
                                 VlanId vlan) {
        Ethernet probePacket = null;

        if (targetIp.isIp4()) {
            // IPv4: Use ARP
            probePacket = buildArpRequest(targetIp, sourceIp, sourceMac,
                                          vlan);
        } else {
            // IPv6: Use Neighbor Discovery
            probePacket = buildNdpRequest(targetIp, sourceIp, sourceMac,
                                          vlan);
        }

        List<Instruction> instructions = new ArrayList<>();
        instructions.add(Instructions.createOutput(port.number()));

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
            .setOutput(port.number())
            .build();

        OutboundPacket outboundPacket =
            new DefaultOutboundPacket(deviceId, treatment,
                                      ByteBuffer.wrap(probePacket.serialize()));

        packetService.emit(outboundPacket);
    }

    private Ethernet buildArpRequest(IpAddress targetIp, IpAddress sourceIp,
                                     MacAddress sourceMac, VlanId vlan) {

        ARP arp = new ARP();
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET)
           .setHardwareAddressLength((byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
           .setProtocolType(ARP.PROTO_TYPE_IP)
           .setProtocolAddressLength((byte) IpAddress.INET_BYTE_LENGTH)
           .setOpCode(ARP.OP_REQUEST);

        arp.setSenderHardwareAddress(sourceMac.toBytes())
           .setSenderProtocolAddress(sourceIp.toOctets())
           .setTargetHardwareAddress(ZERO_MAC_ADDRESS)
           .setTargetProtocolAddress(targetIp.toOctets());

        Ethernet ethernet = new Ethernet();
        ethernet.setEtherType(Ethernet.TYPE_ARP)
                .setDestinationMACAddress(MacAddress.BROADCAST)
                .setSourceMACAddress(sourceMac)
                .setPayload(arp);

        if (!vlan.equals(VlanId.NONE)) {
            ethernet.setVlanID(vlan.toShort());
        }

        ethernet.setPad(true);

        return ethernet;
    }

    private Ethernet buildNdpRequest(IpAddress targetIp, IpAddress sourceIp,
                                     MacAddress sourceMac, VlanId vlan) {

        // Create the Ethernet packet
        Ethernet ethernet = new Ethernet();
        ethernet.setEtherType(Ethernet.TYPE_IPV6)
                .setDestinationMACAddress(MacAddress.BROADCAST)
                .setSourceMACAddress(sourceMac);
        if (!vlan.equals(VlanId.NONE)) {
            ethernet.setVlanID(vlan.toShort());
        }

        //
        // Create the IPv6 packet
        //
        // TODO: The destination IP address should be the
        // solicited-node multicast address
        IPv6 ipv6 = new IPv6();
        ipv6.setSourceAddress(sourceIp.toOctets());
        ipv6.setDestinationAddress(targetIp.toOctets());
        ipv6.setHopLimit((byte) 255);

        // Create the ICMPv6 packet
        ICMP6 icmp6 = new ICMP6();
        icmp6.setIcmpType(ICMP6.NEIGHBOR_SOLICITATION);
        icmp6.setIcmpCode((byte) 0);

        // Create the Neighbor Solication packet
        NeighborSolicitation ns = new NeighborSolicitation();
        ns.setTargetAddress(targetIp.toOctets());
        ns.addOption(NeighborDiscoveryOptions.TYPE_SOURCE_LL_ADDRESS,
                     sourceMac.toBytes());

        icmp6.setPayload(ns);
        ipv6.setPayload(icmp6);
        ethernet.setPayload(ipv6);

        return ethernet;
    }
}
