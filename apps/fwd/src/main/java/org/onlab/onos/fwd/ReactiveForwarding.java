package org.onlab.onos.fwd;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.Instructions;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.packet.InboundPacket;
import org.onlab.onos.net.packet.PacketContext;
import org.onlab.onos.net.packet.PacketProcessor;
import org.onlab.onos.net.packet.PacketService;
import org.onlab.onos.net.topology.TopologyService;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sample reactive forwarding application.
 */
@Component(immediate = true)
public class ReactiveForwarding {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private ReactivePacketProcessor processor = new ReactivePacketProcessor();

    @Activate
    public void activate() {
        packetService.addProcessor(processor, PacketProcessor.ADVISOR_MAX + 1);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;
        log.info("Stopped");
    }


    /**
     * Packet processor responsible for forwarding packets along their paths.
     */
    private class ReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            InboundPacket pkt = context.inPacket();
            HostId id = HostId.hostId(pkt.parsed().getDestinationMAC());

            // Do we know who this is for? If not, flood and bail.
            Host dst = hostService.getHost(id);
            if (dst == null) {
                flood(context);
                return;
            }

            // Are we on an edge switch that our destination is on? If so,
            // simply forward out to the destination and bail.
            if (pkt.receivedFrom().deviceId().equals(dst.location().deviceId())) {
                forward(context, dst.location().port());
                return;
            }

            // Otherwise, get a set of paths that lead from here to the
            // destination edge switch.
            Set<Path> paths = topologyService.getPaths(topologyService.currentTopology(),
                                                       context.inPacket().receivedFrom().deviceId(),
                                                       dst.location().deviceId());
            if (paths.isEmpty()) {
                // If there are no paths, flood and bail.
                flood(context);
                return;
            }

            // Otherwise, pick a path that does not lead back to where we
            // came from; if no such path, flood and bail.
            Path path = pickForwardPath(paths, pkt.receivedFrom().port());
            if (path == null) {
                log.warn("Doh... don't know where to go...");
                flood(context);
                return;
            }

            // Otherwise forward and be done with it.
            forward(context, path.src().port());
        }
    }

    // Selects a path from the given set that does not lead back to the
    // specified port.
    private Path pickForwardPath(Set<Path> paths, PortNumber notToPort) {
        for (Path path : paths) {
            if (!path.src().port().equals(notToPort)) {
                return path;
            }
        }
        return null;
    }

    // Floods the specified packet.
    private void flood(PacketContext context) {
        boolean canBcast = topologyService.isBroadcastPoint(topologyService.currentTopology(),
                                                            context.inPacket().receivedFrom());
        if (canBcast) {
            forward(context, PortNumber.FLOOD);
        } else {
            context.block();
        }
    }

    // Forwards the packet to the specified port.
    private void forward(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().add(Instructions.createOutput(portNumber));
        context.send();
    }

}


