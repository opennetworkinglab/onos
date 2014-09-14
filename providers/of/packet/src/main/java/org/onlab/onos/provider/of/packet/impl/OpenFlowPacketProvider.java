package org.onlab.onos.provider.of.packet.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.nio.ByteBuffer;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.packet.DefaultInboundPacket;
import org.onlab.onos.net.packet.OutboundPacket;
import org.onlab.onos.net.packet.PacketProvider;
import org.onlab.onos.net.packet.PacketProviderRegistry;
import org.onlab.onos.net.packet.PacketProviderService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.of.controller.Dpid;
import org.onlab.onos.of.controller.OpenFlowController;
import org.onlab.onos.of.controller.OpenFlowPacketContext;
import org.onlab.onos.of.controller.PacketListener;
import org.slf4j.Logger;

/**
 * Provider which uses an OpenFlow controller to detect network
 * infrastructure links.
 */
@Component(immediate = true)
public class OpenFlowPacketProvider extends AbstractProvider implements PacketProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    private PacketProviderService providerService;

    private final InternalPacketProvider listener = new InternalPacketProvider();

    /**
     * Creates an OpenFlow link provider.
     */
    public OpenFlowPacketProvider() {
        super(new ProviderId("org.onlab.onos.provider.openflow"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addPacketListener(1, listener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        controller.removePacketListener(listener);
        providerService = null;
        log.info("Stopped");
    }

    @Override
    public void emit(OutboundPacket packet) {

    }


    /**
     * Internal Packet Provider implementation.
     *
     */
    private class InternalPacketProvider implements PacketListener {


        @Override
        public void handlePacket(OpenFlowPacketContext pktCtx) {
            DeviceId id = DeviceId.deviceId(Dpid.uri(pktCtx.dpid().value()));

            DefaultInboundPacket inPkt = new DefaultInboundPacket(
                    new ConnectPoint(id, PortNumber.portNumber(pktCtx.inPort())),
                    pktCtx.parsed(), ByteBuffer.wrap(pktCtx.unparsed()));

            OpenFlowCorePacketContext corePktCtx =
                    new OpenFlowCorePacketContext(0, inPkt, null, false, pktCtx,
                            controller.getSwitch(pktCtx.dpid()));
            providerService.processPacket(corePktCtx);
        }

    }


}
