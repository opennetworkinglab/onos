package org.onlab.onos.fwd;

import static org.slf4j.LoggerFactory.getLogger;

import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.Instructions;
import org.onlab.onos.net.packet.PacketContext;
import org.onlab.onos.net.packet.PacketProcessor;
import org.onlab.onos.net.topology.TopologyService;
import org.slf4j.Logger;

public class ReactivePacketProcessor implements PacketProcessor {

    private final Logger log = getLogger(getClass());
    private final TopologyService topologyService;


    public ReactivePacketProcessor(TopologyService topologyService) {
        this.topologyService = topologyService;
    }


    @Override
    public void process(PacketContext context) {
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
