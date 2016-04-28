/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.provider.bmv2.packet.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.runtime.Bmv2ControlPlaneServer;
import org.onosproject.bmv2.api.runtime.Bmv2Device;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProgrammable;
import org.onosproject.net.packet.PacketProvider;
import org.onosproject.net.packet.PacketProviderRegistry;
import org.onosproject.net.packet.PacketProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Implementation of a packet provider for BMv2.
 */
@Component(immediate = true)
public class Bmv2PacketProvider extends AbstractProvider implements PacketProvider {

    private static final Logger LOG = LoggerFactory.getLogger(Bmv2PacketProvider.class);
    private static final String APP_NAME = "org.onosproject.bmv2";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected Bmv2ControlPlaneServer controlPlaneServer;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private PacketProviderService providerService;

    private InternalPacketListener packetListener = new InternalPacketListener();

    /**
     * Creates a new BMv2 packet provider.
     */
    public Bmv2PacketProvider() {
        super(new ProviderId("bmv2", "org.onosproject.provider.packet"));
    }

    @Activate
    protected void activate() {
        providerService = providerRegistry.register(this);
        coreService.registerApplication(APP_NAME);
        controlPlaneServer.addPacketListener(packetListener);
        LOG.info("Started");
    }

    @Deactivate
    public void deactivate() {
        controlPlaneServer.removePacketListener(packetListener);
        providerRegistry.unregister(this);
        providerService = null;
        LOG.info("Stopped");
    }

    @Override
    public void emit(OutboundPacket packet) {
        if (packet != null) {
            DeviceId did = packet.sendThrough();
            Device device = deviceService.getDevice(did);
            if (device.is(PacketProgrammable.class)) {
                PacketProgrammable packetProgrammable = device.as(PacketProgrammable.class);
                packetProgrammable.emit(packet);
            } else {
                LOG.info("Unable to send packet, no PacketProgrammable behavior for device {}", did);
            }
        }
    }

    /**
     * Internal packet context implementation.
     */
    private class Bmv2PacketContext extends DefaultPacketContext {

        public Bmv2PacketContext(long time, InboundPacket inPkt, OutboundPacket outPkt, boolean block) {
            super(time, inPkt, outPkt, block);
        }

        @Override
        public void send() {
            if (!this.block()) {
                if (this.outPacket().treatment() == null) {
                    TrafficTreatment treatment = (this.treatmentBuilder() == null)
                            ? DefaultTrafficTreatment.emptyTreatment()
                            : this.treatmentBuilder().build();
                    OutboundPacket newPkt = new DefaultOutboundPacket(this.outPacket().sendThrough(),
                                                                      treatment,
                                                                      this.outPacket().data());
                    emit(newPkt);
                } else {
                    emit(outPacket());
                }
            } else {
                LOG.info("Unable to send, packet context not blocked");
            }
        }
    }

    /**
     * Internal packet listener to get packet events from the Bmv2ControlPlaneServer.
     */
    private class InternalPacketListener implements Bmv2ControlPlaneServer.PacketListener {
        @Override
        public void handlePacketIn(Bmv2Device device, int inputPort, long reason, int tableId, int contextId,
                                   ImmutableByteSequence packet) {

            Ethernet eth = new Ethernet();
            eth.deserialize(packet.asArray(), 0, packet.size());

            InboundPacket inPkt = new DefaultInboundPacket(new ConnectPoint(device.asDeviceId(),
                                                                            PortNumber.portNumber(inputPort)),
                                                           eth, ByteBuffer.wrap(packet.asArray()));
            OutboundPacket outPkt = new DefaultOutboundPacket(device.asDeviceId(), null,
                                                              ByteBuffer.wrap(packet.asArray()));
            PacketContext pktCtx = new Bmv2PacketContext(System.currentTimeMillis(), inPkt, outPkt, false);
            providerService.processPacket(pktCtx);
        }
    }
}
