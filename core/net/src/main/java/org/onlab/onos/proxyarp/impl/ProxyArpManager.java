package org.onlab.onos.proxyarp.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.ByteBuffer;
import java.util.Set;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.packet.DefaultOutboundPacket;
import org.onlab.onos.net.packet.PacketService;
import org.onlab.onos.net.proxyarp.ProxyArpService;
import org.onlab.onos.net.topology.TopologyService;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.VlanId;

public class ProxyArpManager implements ProxyArpService {

    private static final String MAC_ADDR_NULL = "Mac address cannot be null.";
    private static final String REQUEST_NULL = "Arp request cannot be null.";
    private static final String REQUEST_NOT_ARP = "Ethernet frame does not contain ARP request.";
    private static final String NOT_ARP_REQUEST = "ARP is not a request.";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Override
    public boolean known(IpPrefix addr) {
        checkNotNull(MAC_ADDR_NULL, addr);
        Set<Host> hosts = hostService.getHostsByIp(addr);
        return !hosts.isEmpty();
    }

    @Override
    public void reply(Ethernet request) {
        checkNotNull(REQUEST_NULL, request);
        checkArgument(request.getEtherType() == Ethernet.TYPE_ARP,
                REQUEST_NOT_ARP);
        ARP arp = (ARP) request.getPayload();
        checkArgument(arp.getOpCode() == ARP.OP_REQUEST, NOT_ARP_REQUEST);

        VlanId vlan = VlanId.vlanId(request.getVlanID());
        Set<Host> hosts = hostService.getHostsByIp(IpPrefix.valueOf(arp
                .getTargetProtocolAddress()));

        Host h = null;
        for (Host host : hosts) {
            if (host.vlan().equals(vlan)) {
                h = host;
                break;
            }
        }

        if (h == null) {
            flood(request);
            return;
        }

        Ethernet arpReply = buildArpReply(h, request);
        // TODO: check send status with host service.
        TrafficTreatment.Builder builder = new DefaultTrafficTreatment.Builder();
        builder.setOutput(h.location().port());
        packetService.emit(new DefaultOutboundPacket(h.location().deviceId(),
                builder.build(), ByteBuffer.wrap(arpReply.serialize())));
    }

    private void flood(Ethernet request) {
        // TODO: flood on all edge ports.
    }

    private Ethernet buildArpReply(Host h, Ethernet request) {
        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(request.getSourceMACAddress());
        eth.setSourceMACAddress(h.mac().getAddress());
        eth.setEtherType(Ethernet.TYPE_ARP);
        ARP arp = new ARP();
        arp.setOpCode(ARP.OP_REPLY);
        arp.setSenderHardwareAddress(h.mac().getAddress());
        arp.setTargetHardwareAddress(request.getSourceMACAddress());

        arp.setTargetProtocolAddress(((ARP) request.getPayload())
                .getSenderProtocolAddress());
        arp.setSenderProtocolAddress(h.ipAddresses().iterator().next().toInt());
        eth.setPayload(arp);
        return eth;
    }
}
