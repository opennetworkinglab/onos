/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.provider.p4runtime.packet.impl;


import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.DefaultPacketContext;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketProgrammable;
import org.onosproject.net.packet.PacketProvider;
import org.onosproject.net.packet.PacketProviderRegistry;
import org.onosproject.net.packet.PacketProviderService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.onosproject.p4runtime.api.P4RuntimeEvent;
import org.onosproject.p4runtime.api.P4RuntimeEventListener;
import org.slf4j.Logger;

import java.nio.ByteBuffer;

import static org.onosproject.net.flow.DefaultTrafficTreatment.emptyTreatment;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of a packet provider for P4Runtime device.
 */
@Component(immediate = true)
public class P4RuntimePacketProvider extends AbstractProvider implements PacketProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected P4RuntimeController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private PacketProviderService providerService;

    private InternalPacketListener packetListener = new InternalPacketListener();

    /**
     * Creates a new P4Runtime packet provider.
     */
    public P4RuntimePacketProvider() {
        super(new ProviderId("p4runtime", "org.onosproject.provider.p4runtime.packet"));
    }

    @Activate
    protected void activate() {
        providerService = providerRegistry.register(this);
        controller.addListener(packetListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        controller.removeListener(packetListener);
        providerRegistry.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    @Override
    public void emit(OutboundPacket packet) {
        if (packet != null) {
            DeviceId deviceId = packet.sendThrough();
            Device device = deviceService.getDevice(deviceId);
            if (device.is(PacketProgrammable.class)) {
                PacketProgrammable packetProgrammable = device.as(PacketProgrammable.class);
                packetProgrammable.emit(packet);
            } else {
                log.warn("No PacketProgrammable behavior for device {}", deviceId);
            }
        }
    }

    /**
     * Internal packet context implementation.
     */
    private class P4RuntimePacketContext extends DefaultPacketContext {

        P4RuntimePacketContext(long time, InboundPacket inPkt, OutboundPacket outPkt, boolean block) {
            super(time, inPkt, outPkt, block);
        }

        @Override
        public void send() {

            if (this.block()) {
                log.info("Unable to send, packet context is blocked");
                return;
            }

            DeviceId deviceId = outPacket().sendThrough();
            ByteBuffer rawData = outPacket().data();

            TrafficTreatment treatment;
            if (outPacket().treatment() == null) {
                treatment = (treatmentBuilder() == null) ? emptyTreatment() : treatmentBuilder().build();
            } else {
                treatment = outPacket().treatment();
            }

            emit(new DefaultOutboundPacket(deviceId, treatment, rawData));
        }
    }

    /**
     * Internal packet listener to handle packet-in events received from the P4Runtime controller.
     */
    private class InternalPacketListener implements P4RuntimeEventListener {


        @Override
        public void event(P4RuntimeEvent event) {
            /**
             * TODO: handle packet-in event.
             * The inputport need parse from metadata() of packet_in event,
             * but support for parsing metadata() is not ready yet.
             */

        }
    }
}
