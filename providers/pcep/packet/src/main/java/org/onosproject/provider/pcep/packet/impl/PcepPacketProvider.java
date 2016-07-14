package org.onosproject.provider.pcep.packet.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TCP;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketProvider;
import org.onosproject.net.packet.PacketProviderRegistry;
import org.onosproject.net.packet.PacketProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClientController;
import org.onosproject.pcep.controller.PcepPacketListener;
import org.slf4j.Logger;

/**
 * Provider which uses an PCEP controller to process packets.
 */
@Component(immediate = true)
@Service
public class PcepPacketProvider extends AbstractProvider implements PacketProvider {

    private static final Logger log = getLogger(PcepPacketProvider.class);
    static final String PROVIDER_ID = "org.onosproject.provider.packet.pcep";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketProviderRegistry packetProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PcepClientController pcepClientController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    PacketProviderService packetProviderService;

    private InnerPacketProvider listener = new InnerPacketProvider();
    public static final String LSRID = "lsrId";
    public static final int PCEP_PORT = 4189;

    /**
     * Creates a Packet provider.
     */
    public PcepPacketProvider() {
        super(new ProviderId("pcep", PROVIDER_ID));
    }

    @Activate
    public void activate() {
        packetProviderService = packetProviderRegistry.register(this);
        pcepClientController.addPacketListener(listener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        packetProviderRegistry.unregister(this);
        pcepClientController.removePacketListener(listener);
        log.info("Stopped");
    }

    private class InnerPacketProvider implements PcepPacketListener {
        @Override
        public void sendPacketIn(PccId pccId) {
            TCP tcp = new TCP();
            // Set the well known PCEP port. To be used to decide to process/discard the packet while processing.
            tcp.setDestinationPort(PCEP_PORT);

            IPv4 ipv4 = new IPv4();
            ipv4.setProtocol(IPv4.PROTOCOL_TCP);
            ipv4.setPayload(tcp);

            Ethernet eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_IPV4);
            eth.setDestinationMACAddress(MacAddress.NONE);
            eth.setPayload(ipv4);

            // Get lsrId of the PCEP client from the PCC ID. Session info is based on lsrID.
            String lsrId = String.valueOf(pccId.ipAddress());
            DeviceId pccDeviceId = DeviceId.deviceId(lsrId);

            InboundPacket inPkt = new DefaultInboundPacket(new ConnectPoint(pccDeviceId,
                                                                            PortNumber.portNumber(PCEP_PORT)),
                                                           eth, null);

            packetProviderService.processPacket(new PcepPacketContext(inPkt, null));
        }
    }

    // Minimal PacketContext to make core and applications happy.
    private final class PcepPacketContext extends DefaultPacketContext {
        private PcepPacketContext(InboundPacket inPkt, OutboundPacket outPkt) {
            super(System.currentTimeMillis(), inPkt, outPkt, false);
        }

        @Override
        public void send() {
            // We don't send anything out.
            return;
        }
    }

    @Override
    public void emit(OutboundPacket packet) {
        // Nothing to emit
        return;

    }
}
