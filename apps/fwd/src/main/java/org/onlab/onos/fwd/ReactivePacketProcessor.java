package org.onlab.onos.fwd;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.Instructions;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.packet.InboundPacket;
import org.onlab.onos.net.packet.PacketContext;
import org.onlab.onos.net.packet.PacketProcessor;
import org.onlab.onos.net.topology.TopologyService;
import org.onlab.packet.VLANID;
import org.slf4j.Logger;

public class ReactivePacketProcessor implements PacketProcessor {

    private final Logger log = getLogger(getClass());
    private final TopologyService topologyService;
    private final HostService hostService;


    public ReactivePacketProcessor(TopologyService topologyService, HostService hostService) {
        this.topologyService = topologyService;
        this.hostService = hostService;
    }


    @Override
    public void process(PacketContext context) {
        InboundPacket pkt = context.inPacket();
        HostId id = HostId.hostId(pkt.parsed().getDestinationMAC(), VLANID.vlanId((short) -1));
        Host dst = hostService.getHost(id);
        if (dst == null) {
            flood(context);
            return;
        }

        Set<Path> p = null;
        if (pkt.receivedFrom().deviceId().equals(dst.location().deviceId())) {
            context.treatmentBuilder().add(Instructions.createOutput(dst.location().port()));
            context.send();
            return;
        } else {
            p = topologyService.getPaths(topologyService.currentTopology(),
                    context.inPacket().receivedFrom().deviceId(), dst.location().deviceId());
        }

        if (p.isEmpty()) {
            flood(context);
        } else {
            Path p1 = p.iterator().next();
            context.treatmentBuilder().add(Instructions.createOutput(p1.src().port()));
            context.send();
        }

    }

    private void flood(PacketContext context) {
        boolean canBcast = topologyService.isBroadcastPoint(topologyService.currentTopology(),
                context.inPacket().receivedFrom());
        if (canBcast) {
            context.treatmentBuilder().add(Instructions.createOutput(PortNumber.FLOOD));
            context.send();
        } else {
            context.block();
        }
    }

}
