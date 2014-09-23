package org.onlab.onos.net.host.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.host.HostStore;
import org.onlab.onos.net.host.PortAddresses;
import org.onlab.onos.net.packet.DefaultOutboundPacket;
import org.onlab.onos.net.packet.OutboundPacket;
import org.onlab.onos.net.packet.PacketService;
import org.onlab.onos.net.topology.TopologyService;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.util.Timer;

/**
 * Monitors hosts on the dataplane to detect changes in host data.
 * <p/>
 * The HostMonitor can monitor hosts that have already been detected for
 * changes. At an application's request, it can also monitor and actively
 * probe for hosts that have not yet been detected (specified by IP address).
 */
public class HostMonitor implements TimerTask {

    private static final byte[] DEFAULT_MAC_ADDRESS =
            MacAddress.valueOf("00:00:00:00:00:01").getAddress();

    private static final byte[] ZERO_MAC_ADDRESS =
            MacAddress.valueOf("00:00:00:00:00:00").getAddress();

    // TODO put on Ethernet
    private static final byte[] BROADCAST_MAC =
            MacAddress.valueOf("ff:ff:ff:ff:ff:ff").getAddress();

    private final HostService hostService;
    private final TopologyService topologyService;
    private final DeviceService deviceService;
    private final HostProvider hostProvider;
    private final PacketService packetService;
    private final HostStore hostStore;

    private final Set<IpAddress> monitoredAddresses;

    private final long probeRate;

    private Timeout timeout;

    public HostMonitor(HostService hostService, TopologyService topologyService,
                       DeviceService deviceService,
                       HostProvider hostProvider, PacketService packetService,
                       HostStore hostStore) {
        this.hostService = hostService;
        this.topologyService = topologyService;
        this.deviceService = deviceService;
        this.hostProvider = hostProvider;
        this.packetService = packetService;
        this.hostStore = hostStore;

        monitoredAddresses = new HashSet<>();

        probeRate = 30000; // milliseconds

        timeout = Timer.getTimer().newTimeout(this, 0, TimeUnit.MILLISECONDS);
    }

    public void addMonitoringFor(IpAddress ip) {
        monitoredAddresses.add(ip);
    }

    public void stopMonitoring(IpAddress ip) {
        monitoredAddresses.remove(ip);
    }

    public void shutdown() {
        timeout.cancel();
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        for (IpAddress ip : monitoredAddresses) {
            Set<Host> hosts = Collections.emptySet(); //TODO hostService.getHostsByIp(ip);

            if (hosts.isEmpty()) {
                sendArpRequest(ip);
            } else {
                for (Host host : hosts) {
                    hostProvider.triggerProbe(host);
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
                PortAddresses addresses = hostStore.getAddressBindingsForPort(cp);

                if (addresses.ip().contains(targetIp)) {
                    sendProbe(device.id(), port, addresses, targetIp);
                }
            }
        }

        // TODO case where no address was found.
        // Broadcast out internal edge ports?
    }

    private void sendProbe(DeviceId deviceId, Port port, PortAddresses portAddresses,
            IpAddress targetIp) {
        Ethernet arpPacket = createArpFor(targetIp, portAddresses);

        List<Instruction> instructions = new ArrayList<>();
        instructions.add(Instructions.createOutput(port.number()));

        TrafficTreatment treatment =
                new DefaultTrafficTreatment.Builder()
                .add(Instructions.createOutput(port.number()))
                .build();

        OutboundPacket outboundPacket =
                new DefaultOutboundPacket(deviceId, treatment,
                        ByteBuffer.wrap(arpPacket.serialize()));

        packetService.emit(outboundPacket);
    }

    private Ethernet createArpFor(IpAddress targetIp, PortAddresses portAddresses) {

        ARP arp = new ARP();
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET)
           .setHardwareAddressLength((byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
           .setProtocolType(ARP.PROTO_TYPE_IP)
           .setProtocolAddressLength((byte) IpPrefix.INET_LEN);

        byte[] sourceMacAddress;
        if (portAddresses.mac() == null) {
            sourceMacAddress = DEFAULT_MAC_ADDRESS;
        } else {
            sourceMacAddress = portAddresses.mac().getAddress();
        }

        arp.setSenderHardwareAddress(sourceMacAddress)
           .setSenderProtocolAddress(portAddresses.ip().toOctets())
           .setTargetHardwareAddress(ZERO_MAC_ADDRESS)
           .setTargetProtocolAddress(targetIp.toOctets());

        Ethernet ethernet = new Ethernet();
        ethernet.setEtherType(Ethernet.TYPE_ARP)
                .setDestinationMACAddress(BROADCAST_MAC)
                .setSourceMACAddress(sourceMacAddress)
                .setPayload(arp);

        return ethernet;
    }
}
