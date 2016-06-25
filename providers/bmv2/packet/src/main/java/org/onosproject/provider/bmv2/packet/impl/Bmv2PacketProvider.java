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
import org.onosproject.bmv2.api.runtime.Bmv2Device;
import org.onosproject.bmv2.api.service.Bmv2Controller;
import org.onosproject.bmv2.api.service.Bmv2PacketListener;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
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
import java.util.Optional;

import static org.onosproject.net.PortNumber.FLOOD;
import static org.onosproject.net.flow.DefaultTrafficTreatment.emptyTreatment;
import static org.onosproject.net.flow.instructions.Instruction.Type.OUTPUT;
import static org.onosproject.net.flow.instructions.Instructions.OutputInstruction;

/**
 * Implementation of a packet provider for BMv2.
 */
@Component(immediate = true)
public class Bmv2PacketProvider extends AbstractProvider implements PacketProvider {

    private final Logger log = LoggerFactory.getLogger(Bmv2PacketProvider.class);
    private static final String APP_NAME = "org.onosproject.bmv2";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected Bmv2Controller controller;

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
        controller.addPacketListener(packetListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        controller.removePacketListener(packetListener);
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
                log.info("No PacketProgrammable behavior for device {}", deviceId);
            }
        }
    }

    /**
     * Internal packet context implementation.
     */
    private class Bmv2PacketContext extends DefaultPacketContext {

        Bmv2PacketContext(long time, InboundPacket inPkt, OutboundPacket outPkt, boolean block) {
            super(time, inPkt, outPkt, block);
        }

        @Override
        public void send() {

            if (this.block()) {
                log.info("Unable to send, packet context not blocked");
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

            // BMv2 doesn't support FLOOD for packet-outs.
            // Workaround here is to perform multiple emits, one for each device port != packet inPort.
            Optional<OutputInstruction> floodInst = treatment.allInstructions()
                    .stream()
                    .filter(i -> i.type().equals(OUTPUT))
                    .map(i -> (OutputInstruction) i)
                    .filter(i -> i.port().equals(FLOOD))
                    .findAny();

            if (floodInst.isPresent() && treatment.allInstructions().size() == 1) {
                // Only one instruction and is FLOOD. Do the trick.
                PortNumber inPort = inPacket().receivedFrom().port();
                deviceService.getPorts(outPacket().sendThrough())
                        .stream()
                        .map(Port::number)
                        .filter(port -> !port.equals(inPort))
                        .map(outPort -> DefaultTrafficTreatment.builder().setOutput(outPort).build())
                        .map(outTreatment -> new DefaultOutboundPacket(deviceId, outTreatment, rawData))
                        .forEach(Bmv2PacketProvider.this::emit);
            } else {
                // Not FLOOD treatment, what to do is up to driver.
                emit(new DefaultOutboundPacket(deviceId, treatment, rawData));
            }
        }
    }

    /**
     * Internal packet listener to handle packet-in events received from the BMv2 controller.
     */
    private class InternalPacketListener implements Bmv2PacketListener {

        @Override
        public void handlePacketIn(Bmv2Device device, int inputPort, ImmutableByteSequence packet) {
            Ethernet ethPkt = new Ethernet();
            ethPkt.deserialize(packet.asArray(), 0, packet.size());

            DeviceId deviceId = device.asDeviceId();
            ConnectPoint receivedFrom = new ConnectPoint(deviceId, PortNumber.portNumber(inputPort));

            ByteBuffer rawData = ByteBuffer.wrap(packet.asArray());

            InboundPacket inPkt = new DefaultInboundPacket(receivedFrom, ethPkt, rawData);
            OutboundPacket outPkt = new DefaultOutboundPacket(deviceId, null, rawData);

            PacketContext pktCtx = new Bmv2PacketContext(System.currentTimeMillis(), inPkt, outPkt, false);

            providerService.processPacket(pktCtx);
        }
    }
}
