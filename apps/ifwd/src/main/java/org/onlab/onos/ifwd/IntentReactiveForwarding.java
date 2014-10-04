package org.onlab.onos.ifwd;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.HostToHostIntent;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.packet.DefaultOutboundPacket;
import org.onlab.onos.net.packet.InboundPacket;
import org.onlab.onos.net.packet.OutboundPacket;
import org.onlab.onos.net.packet.PacketContext;
import org.onlab.onos.net.packet.PacketProcessor;
import org.onlab.onos.net.packet.PacketService;
import org.onlab.onos.net.topology.TopologyService;
import org.onlab.packet.Ethernet;
import org.slf4j.Logger;

/**
 * WORK-IN-PROGRESS: Sample reactive forwarding application using intent framework.
 */
@Component(immediate = true)
public class IntentReactiveForwarding {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private ReactivePacketProcessor processor = new ReactivePacketProcessor();

    private ApplicationId appId;
    private static long intentId = 1;

    @Activate
    public void activate() {
        appId = ApplicationId.getAppId();
        packetService.addProcessor(processor, PacketProcessor.ADVISOR_MAX + 2);
        log.info("Started with Application ID {}", appId.id());
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
            // Stop processing if the packet has been handled, since we
            // can't do any more to it.
            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            HostId srcId = HostId.hostId(ethPkt.getSourceMAC());
            HostId dstId = HostId.hostId(ethPkt.getDestinationMAC());

            // Do we know who this is for? If not, flood and bail.
            Host dst = hostService.getHost(dstId);
            if (dst == null) {
                flood(context);
                return;
            }

            // Otherwise forward and be done with it.
            setUpConnectivity(context, srcId, dstId);
            forwardPacketToDst(context, dst);
        }
    }

    // Floods the specified packet if permissible.
    private void flood(PacketContext context) {
        if (topologyService.isBroadcastPoint(topologyService.currentTopology(),
                                             context.inPacket().receivedFrom())) {
            packetOut(context, PortNumber.FLOOD);
        } else {
            context.block();
        }
    }

    // Sends a packet out the specified port.
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

    private void forwardPacketToDst(PacketContext context, Host dst) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(dst.location().port()).build();
        OutboundPacket packet = new DefaultOutboundPacket(dst.location().deviceId(),
                treatment, context.inPacket().unparsed());
        packetService.emit(packet);
        log.info("sending packet: {}", packet);
    }

    // Install a rule forwarding the packet to the specified port.
    private void setUpConnectivity(PacketContext context, HostId srcId, HostId dstId) {
        TrafficSelector selector = DefaultTrafficSelector.builder().build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

        HostToHostIntent intent =
                new HostToHostIntent(new IntentId(intentId++), srcId, dstId,
                                     selector, treatment);

        intentService.submit(intent);
    }

}


