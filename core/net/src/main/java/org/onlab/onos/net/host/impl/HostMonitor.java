package org.onlab.onos.net.host.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.onos.net.flow.instructions.Instructions;
import org.onlab.onos.net.host.HostProvider;
import org.onlab.onos.net.host.PortAddresses;
import org.onlab.onos.net.packet.DefaultOutboundPacket;
import org.onlab.onos.net.packet.OutboundPacket;
import org.onlab.onos.net.packet.PacketService;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors hosts on the dataplane to detect changes in host data.
 * <p/>
 * The HostMonitor can monitor hosts that have already been detected for
 * changes. At an application's request, it can also monitor and actively
 * probe for hosts that have not yet been detected (specified by IP address).
 */
public class HostMonitor implements TimerTask {
    private static final Logger log = LoggerFactory.getLogger(HostMonitor.class);

    private static final byte[] ZERO_MAC_ADDRESS =
            MacAddress.valueOf("00:00:00:00:00:00").getAddress();

    // TODO put on Ethernet
    private static final byte[] BROADCAST_MAC =
            MacAddress.valueOf("ff:ff:ff:ff:ff:ff").getAddress();

    private DeviceService deviceService;
    private PacketService packetService;
    private HostManager hostManager;

    private final Set<IpAddress> monitoredAddresses;

    private final Map<ProviderId, HostProvider> hostProviders;

    private final long probeRate;

    private final Timeout timeout;

    public HostMonitor(
            DeviceService deviceService,
            PacketService packetService,
            HostManager hostService) {

        this.deviceService = deviceService;
        this.packetService = packetService;
        this.hostManager = hostService;

        monitoredAddresses = new HashSet<>();
        hostProviders = new ConcurrentHashMap<>();

        probeRate = 30000; // milliseconds

        timeout = Timer.getTimer().newTimeout(this, 0, TimeUnit.MILLISECONDS);

        addDefaultAddresses();
    }

    private void addDefaultAddresses() {
        //monitoredAddresses.add(IpAddress.valueOf("10.0.0.1"));
    }

    void addMonitoringFor(IpAddress ip) {
        monitoredAddresses.add(ip);
    }

    void stopMonitoring(IpAddress ip) {
        monitoredAddresses.remove(ip);
    }

    void shutdown() {
        timeout.cancel();
    }

    void registerHostProvider(HostProvider provider) {
        hostProviders.put(provider.id(), provider);
    }

    void unregisterHostProvider(HostProvider provider) {
        // TODO find out how to call this
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        for (IpAddress ip : monitoredAddresses) {
            // TODO have to convert right now because the HostService API uses IpPrefix
            IpPrefix prefix = IpPrefix.valueOf(ip.toOctets());

            Set<Host> hosts = hostManager.getHostsByIp(prefix);

            if (hosts.isEmpty()) {
                sendArpRequest(ip);
            } else {
                for (Host host : hosts) {
                    HostProvider provider = hostProviders.get(host.providerId());
                    if (provider != null) {
                        provider.triggerProbe(host);
                    }
                }
            }
        }

        timeout = Timer.getTimer().newTimeout(this, probeRate, TimeUnit.MILLISECONDS);
    }

    /**
     * Sends an ARP request for the given IP address.
     *
     * @param targetIp IP address to ARP for
     */
    private void sendArpRequest(IpAddress targetIp) {
        // Find ports with an IP address in the target's subnet and sent ARP
        // probes out those ports.
        for (Device device : deviceService.getDevices()) {
            for (Port port : deviceService.getPorts(device.id())) {
                ConnectPoint cp = new ConnectPoint(device.id(), port.number());
                PortAddresses addresses = hostManager.getAddressBindingsForPort(cp);

                for (IpPrefix prefix : addresses.ips()) {
                    if (prefix.contains(targetIp)) {
                        sendProbe(device.id(), port, targetIp,
                                prefix.toIpAddress(), addresses.mac());
                    }
                }
            }
        }
    }

    private void sendProbe(DeviceId deviceId, Port port, IpAddress targetIp,
            IpAddress sourceIp, MacAddress sourceMac) {
        Ethernet arpPacket = buildArpRequest(targetIp, sourceIp, sourceMac);

        List<Instruction> instructions = new ArrayList<>();
        instructions.add(Instructions.createOutput(port.number()));

        TrafficTreatment treatment = new DefaultTrafficTreatment.Builder()
        .setOutput(port.number())
        .build();

        OutboundPacket outboundPacket =
                new DefaultOutboundPacket(deviceId, treatment,
                        ByteBuffer.wrap(arpPacket.serialize()));

        packetService.emit(outboundPacket);
    }

    private Ethernet buildArpRequest(IpAddress targetIp, IpAddress sourceIp,
            MacAddress sourceMac) {

        ARP arp = new ARP();
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET)
           .setHardwareAddressLength((byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
           .setProtocolType(ARP.PROTO_TYPE_IP)
           .setProtocolAddressLength((byte) IpPrefix.INET_LEN)
           .setOpCode(ARP.OP_REQUEST);

        arp.setSenderHardwareAddress(sourceMac.getAddress())
           .setSenderProtocolAddress(sourceIp.toOctets())
           .setTargetHardwareAddress(ZERO_MAC_ADDRESS)
           .setTargetProtocolAddress(targetIp.toOctets());

        Ethernet ethernet = new Ethernet();
        ethernet.setEtherType(Ethernet.TYPE_ARP)
                .setDestinationMACAddress(BROADCAST_MAC)
                .setSourceMACAddress(sourceMac.getAddress())
                .setPayload(arp);

        return ethernet;
    }
}
