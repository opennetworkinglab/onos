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

package org.onosproject.drivers.bmv2;

import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.runtime.Bmv2Client;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.ctl.Bmv2ThriftClient;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketProgrammable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.toIntExact;
import static org.onosproject.net.PortNumber.FLOOD;
import static org.onosproject.net.flow.instructions.Instruction.Type.OUTPUT;

/**
 * Packet programmable device behaviour implementation for BMv2.
 */
public class Bmv2PacketProgrammable extends AbstractHandlerBehaviour implements PacketProgrammable {

    private static final Logger LOG =
            LoggerFactory.getLogger(Bmv2PacketProgrammable.class);

    @Override
    public void emit(OutboundPacket packet) {

        TrafficTreatment treatment = packet.treatment();

        treatment.allInstructions().forEach(inst -> {
            if (inst.type().equals(OUTPUT)) {
                Instructions.OutputInstruction outInst = (Instructions.OutputInstruction) inst;
                if (outInst.port().isLogical()) {
                    if (outInst.port() == FLOOD) {
                        // TODO: implement flood
                        LOG.info("Flood not implemented", outInst);
                    }
                    LOG.info("Output on logical port not supported: {}", outInst);
                } else {
                    try {
                        long longPort = outInst.port().toLong();
                        int portNumber = toIntExact(longPort);
                        send(portNumber, packet);
                    } catch (ArithmeticException e) {
                        LOG.error("Port number overflow! Cannot send packet on port {} (long), as the bmv2" +
                                          " device only accepts int port values.");
                    }
                }
            } else {
                LOG.info("Instruction type not supported: {}", inst.type().name());
            }
        });
    }

    private void send(int port, OutboundPacket packet) {

        DeviceId deviceId = handler().data().deviceId();

        Bmv2Client deviceClient;
        try {
            deviceClient = Bmv2ThriftClient.of(deviceId);
        } catch (Bmv2RuntimeException e) {
            LOG.error("Failed to connect to Bmv2 device", e);
            return;
        }

        ImmutableByteSequence bs = ImmutableByteSequence.copyFrom(packet.data());
        try {
            deviceClient.transmitPacket(port, bs);
        } catch (Bmv2RuntimeException e) {
            LOG.info("Unable to push packet to device: deviceId={}, packet={}", deviceId, bs);
        }
    }
}
