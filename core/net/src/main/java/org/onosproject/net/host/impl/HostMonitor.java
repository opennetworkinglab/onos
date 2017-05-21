/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onlab.util.SharedScheduledExecutors;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Monitors hosts on the dataplane to detect changes in host data.
 * <p>
 * The HostMonitor can monitor hosts that have already been detected for
 * changes. At an application's request, it can also monitor and actively
 * probe for hosts that have not yet been detected (specified by IP address).
 * </p>
 */
public class HostMonitor implements Runnable {

    private Logger log = LoggerFactory.getLogger(getClass());

    private PacketService packetService;
    private HostManager hostManager;
    private InterfaceService interfaceService;
    private EdgePortService edgePortService;

    private final Set<IpAddress> monitoredAddresses;

    private final ConcurrentMap<ProviderId, HostProvider> hostProviders;

    private static final long DEFAULT_PROBE_RATE = 30000; // milliseconds
    private static final byte[] ZERO_MAC_ADDRESS = MacAddress.ZERO.toBytes();
    private long probeRate = DEFAULT_PROBE_RATE;

    private ScheduledFuture<?> timeout;

    /**
     * Creates a new host monitor.
     *
     * @param packetService packet service used to send packets on the data plane
     * @param hostManager host manager used to look up host information and
     * probe existing hosts
     * @param interfaceService interface service for interface information
     * @param edgePortService  edge port service
     */
    public HostMonitor(PacketService packetService, HostManager hostManager,
                       InterfaceService interfaceService,
                       EdgePortService edgePortService) {

        this.packetService = packetService;
        this.hostManager = hostManager;
        this.interfaceService = interfaceService;
        this.edgePortService = edgePortService;

        monitoredAddresses = Collections.newSetFromMap(new ConcurrentHashMap<>());
        hostProviders = new ConcurrentHashMap<>();
    }

    /**
     * Adds an IP address to be monitored by the host monitor. The monitor will
     * periodically probe the host to detect changes.
     *
     * @param ip IP address of the host to monitor
     */
    void addMonitoringFor(IpAddress ip) {
        if (monitoredAddresses.add(ip)) {
            probe(ip);
        }
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
                timeout = SharedScheduledExecutors.newTimeout(this, 0, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Stops the host monitor.
     */
    void shutdown() {
        synchronized (this) {
            timeout.cancel(true);
            timeout = null;
        }
    }

    /*
     * Sets the probe rate.
     */
    void setProbeRate(long probeRate) {
        this.probeRate = probeRate;
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
    public void run() {
        monitoredAddresses.forEach(this::probe);

        synchronized (this) {
            this.timeout = SharedScheduledExecutors.newTimeout(this, probeRate, TimeUnit.MILLISECONDS);
        }
    }

    private void probe(IpAddress ip) {
        Set<Host> hosts = hostManager.getHostsByIp(ip);

        if (hosts.isEmpty()) {
            sendRequest(ip);
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

    /**
     * Sends an ARP or NDP request for the given IP address.
     *
     * @param targetIp IP address to send the request for
     */
    private void sendRequest(IpAddress targetIp) {
        interfaceService.getMatchingInterfaces(targetIp).forEach(intf -> {
            if (!edgePortService.isEdgePoint(intf.connectPoint())) {
                log.warn("Aborting attempt to send probe out non-edge port: {}", intf);
                return;
            }

            intf.ipAddressesList().stream()
                    .filter(ia -> ia.subnetAddress().contains(targetIp))
                    .forEach(ia -> {
                        // Use DAD to probe when interface MAC is not supplied,
                        // such that host will not learn ONOS dummy MAC from the probe.
                        IpAddress sourceIp;
                        if (!MacAddress.NONE.equals(intf.mac())) {
                            sourceIp = ia.ipAddress();
                        } else {
                            sourceIp = targetIp.isIp4() ? Ip4Address.ZERO : Ip6Address.ZERO;
                        }

                        log.debug("Sending probe for target:{} out of intf:{} vlan:{}",
                                targetIp, intf.connectPoint(), intf.vlan());
                        sendProbe(intf.connectPoint(), targetIp, sourceIp,
                                intf.mac(), intf.vlan());
                        // account for use-cases where tagged-vlan config is used
                        if (!intf.vlanTagged().isEmpty()) {
                            intf.vlanTagged().forEach(tag -> {
                                log.debug("Sending probe for target:{} out of intf:{} vlan:{}",
                                        targetIp, intf.connectPoint(), tag);
                                sendProbe(intf.connectPoint(), targetIp, sourceIp,
                                        intf.mac(), tag);
                            });
                        }
                    });
        });
    }

    public void sendProbe(ConnectPoint connectPoint,
                          IpAddress targetIp,
                          IpAddress sourceIp,
                          MacAddress sourceMac,
                          VlanId vlan) {
        Ethernet probePacket;

        if (targetIp.isIp4()) {
            // IPv4: Use ARP
            probePacket = buildArpRequest(targetIp, sourceIp, sourceMac, vlan);
        } else {
             // IPv6: Use Neighbor Discovery. According to the NDP protocol,
             // we should use the solicitation node address as IPv6 destination
             // and the multicast mac address as Ethernet destination.
            byte[] destIp = IPv6.getSolicitNodeAddress(targetIp.toOctets());
            probePacket = NeighborSolicitation.buildNdpSolicit(
                    targetIp.toOctets(),
                    sourceIp.toOctets(),
                    destIp,
                    sourceMac.toBytes(),
                    IPv6.getMCastMacAddress(destIp),
                    vlan
            );
        }

        if (probePacket == null) {
            log.warn("Not able to build the probe packet");
            return;
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
            .setOutput(connectPoint.port())
            .build();

        OutboundPacket outboundPacket =
            new DefaultOutboundPacket(connectPoint.deviceId(), treatment,
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

}
