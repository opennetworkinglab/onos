package org.onlab.onos.provider.of.packet.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.nio.ByteBuffer;
import java.util.Collections;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.onos.net.flow.instructions.Instructions.OutputInstruction;
import org.onlab.onos.net.packet.DefaultInboundPacket;
import org.onlab.onos.net.packet.OutboundPacket;
import org.onlab.onos.net.packet.PacketProvider;
import org.onlab.onos.net.packet.PacketProviderRegistry;
import org.onlab.onos.net.packet.PacketProviderService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.openflow.controller.DefaultOpenFlowPacketContext;
import org.onlab.onos.openflow.controller.Dpid;
import org.onlab.onos.openflow.controller.OpenFlowController;
import org.onlab.onos.openflow.controller.OpenFlowPacketContext;
import org.onlab.onos.openflow.controller.OpenFlowSwitch;
import org.onlab.onos.openflow.controller.PacketListener;
import org.onlab.packet.Ethernet;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.ver10.OFFactoryVer10;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;

import static org.onlab.onos.openflow.controller.RoleState.*;


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
        super(new ProviderId("of", "org.onlab.onos.provider.openflow"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addPacketListener(20, listener);
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
        DeviceId devId = packet.sendThrough();
        String scheme = devId.toString().split(":")[0];

        if (!scheme.equals(this.id().scheme())) {
            throw new IllegalArgumentException(
                    "Don't know how to handle Device with scheme " + scheme);
        }

        Dpid dpid = Dpid.dpid(devId.uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);
        if (sw == null) {
            log.warn("Device {} isn't available?", devId);
            return;
        } else if (sw.getRole().equals(SLAVE)) {
            log.warn("Can't write to Device {} as slave", devId);
            return;
        }

        Ethernet eth = new Ethernet();
        eth.deserialize(packet.data().array(), 0, packet.data().array().length);
        OFPortDesc p = null;
        for (Instruction inst : packet.treatment().instructions()) {
            if (inst.type().equals(Instruction.Type.OUTPUT)) {
                p = portDesc(((OutputInstruction) inst).port());
                if (!sw.getPorts().contains(p)) {
                    log.warn("Tried to write out non-existint port {}", p.getPortNo());
                    continue;
                }
                OFPacketOut po = packetOut(sw, eth, p.getPortNo());
                sw.sendMsg(po);
            }
        }

    }

    private OFPortDesc portDesc(PortNumber port) {
        OFPortDesc.Builder builder = OFFactoryVer10.INSTANCE.buildPortDesc();
        builder.setPortNo(OFPort.of((int) port.toLong()));

        return builder.build();
    }

    private OFPacketOut packetOut(OpenFlowSwitch sw, Ethernet eth, OFPort out) {
        OFPacketOut.Builder builder = sw.factory().buildPacketOut();
        OFAction act = sw.factory().actions()
                .buildOutput()
                .setPort(out)
                .build();
        return builder
                .setBufferId(OFBufferId.NO_BUFFER)
                .setInPort(OFPort.NO_MASK)
                .setActions(Collections.singletonList(act))
                .setData(eth.serialize())
                .build();
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
                    new OpenFlowCorePacketContext(System.currentTimeMillis(),
                            inPkt, null, pktCtx.isHandled(), pktCtx);
            providerService.processPacket(corePktCtx);
        }

    }


}
