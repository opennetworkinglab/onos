/*
 * Copyright 2014-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.provider.of.packet.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketProvider;
import org.onosproject.net.packet.PacketProviderRegistry;
import org.onosproject.net.packet.PacketProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowPacketContext;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.PacketListener;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.ver10.OFFactoryVer10;
import org.projectfloodlight.openflow.protocol.ver15.OFFactoryVer15;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Collections;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Provider which uses an OpenFlow controller to detect network
 * infrastructure links.
 */
@Component(immediate = true)
public class OpenFlowPacketProvider extends AbstractProvider implements PacketProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenFlowController controller;

    private PacketProviderService providerService;

    private final InternalPacketProvider listener = new InternalPacketProvider();

    /**
     * Creates an OpenFlow link provider.
     */
    public OpenFlowPacketProvider() {
        super(new ProviderId("of", "org.onosproject.provider.openflow"));
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
        }

        OFPort inPort;
        if (packet.inPort() != null) {
            inPort = portDesc(packet.inPort()).getPortNo();
        } else {
            inPort = OFPort.CONTROLLER;
        }

        //Ethernet eth = new Ethernet();
        //eth.deserialize(packet.data().array(), 0, packet.data().array().length);
        OFPortDesc p = null;
        for (Instruction inst : packet.treatment().allInstructions()) {
            if (inst.type().equals(Instruction.Type.OUTPUT)) {
                p = portDesc(((OutputInstruction) inst).port());

                OFPacketOut po = packetOut(sw, packet.data().array(), p.getPortNo(), inPort);
                sw.sendMsg(po);
            }
        }

    }

    private OFPortDesc portDesc(PortNumber port) {
        OFPortDesc.Builder builder = OFFactoryVer10.INSTANCE.buildPortDesc();
        builder.setPortNo(OFPort.of((int) port.toLong()));

        return builder.build();
    }

    private OFPacketOut packetOut(OpenFlowSwitch sw, byte[] eth, OFPort out, OFPort inPort) {
        OFPacketOut.Builder builder = sw.factory().buildPacketOut();
        OFAction act = sw.factory().actions()
                .buildOutput()
                .setPort(out)
                .build();
        builder.setBufferId(OFBufferId.NO_BUFFER)
                .setActions(Collections.singletonList(act))
                .setData(eth);
        int wireVersion = sw.factory().getVersion().getWireVersion();
        if (wireVersion <= OFVersion.OF_14.getWireVersion()) {
            builder.setInPort(inPort);
        } else if (wireVersion <= OFVersion.OF_15.getWireVersion()) {
            Match m = OFFactoryVer15.INSTANCE.matchWildcardAll().createBuilder()
                    .setExact(MatchField.IN_PORT, inPort)
                    .build();
            builder.setMatch(m);
        }

        return builder.build();
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
                    pktCtx.parsed(), ByteBuffer.wrap(pktCtx.unparsed()),
                    pktCtx.cookie());

            DefaultOutboundPacket outPkt = null;
            if (!pktCtx.isBuffered()) {
                outPkt = new DefaultOutboundPacket(id, null,
                        ByteBuffer.wrap(pktCtx.unparsed()));
            }

            OpenFlowCorePacketContext corePktCtx =
                    new OpenFlowCorePacketContext(System.currentTimeMillis(),
                            inPkt, outPkt, pktCtx.isHandled(), pktCtx);
            providerService.processPacket(corePktCtx);
        }

    }


}
