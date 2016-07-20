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

import org.onlab.osgi.ServiceNotFoundException;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.bmv2.api.runtime.Bmv2DeviceAgent;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.api.service.Bmv2Controller;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketProgrammable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.lang.Math.toIntExact;
import static java.util.stream.Collectors.toList;
import static org.onosproject.net.flow.instructions.Instruction.Type.OUTPUT;
import static org.onosproject.net.flow.instructions.Instructions.OutputInstruction;

/**
 * Implementation of the packet programmable behaviour for BMv2.
 */
public class Bmv2PacketProgrammable extends AbstractHandlerBehaviour implements PacketProgrammable {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void emit(OutboundPacket packet) {

        TrafficTreatment treatment = packet.treatment();

        // BMv2 supports only OUTPUT instructions.
        List<OutputInstruction> outInstructions = treatment.allInstructions()
                .stream()
                .filter(i -> i.type().equals(OUTPUT))
                .map(i -> (OutputInstruction) i)
                .collect(toList());

        if (treatment.allInstructions().size() != outInstructions.size()) {
            // There are other instructions that are not of type OUTPUT
            log.warn("Dropping emit request, treatment nor supported: {}", treatment);
            return;
        }

        outInstructions.forEach(outInst -> {
            if (outInst.port().isLogical()) {
                log.warn("Dropping emit request, logical port not supported: {}", outInst.port());
            } else {
                try {
                    int portNumber = toIntExact(outInst.port().toLong());
                    send(portNumber, packet);
                } catch (ArithmeticException e) {
                    log.error("Dropping emit request, port number too big: {}", outInst.port().toLong());
                }
            }
        });
    }

    private void send(int port, OutboundPacket packet) {

        DeviceId deviceId = handler().data().deviceId();

        Bmv2Controller controller;
        try {
            controller = handler().get(Bmv2Controller.class);
        } catch (ServiceNotFoundException e) {
            log.warn(e.getMessage());
            return;
        }

        Bmv2DeviceAgent deviceAgent;
        try {
            deviceAgent = controller.getAgent(deviceId);
        } catch (Bmv2RuntimeException e) {
            log.error("Failed to get Bmv2 device agent for {}: {}", deviceId, e.explain());
            return;
        }

        ImmutableByteSequence bs = ImmutableByteSequence.copyFrom(packet.data());
        try {
            deviceAgent.transmitPacket(port, bs);
        } catch (Bmv2RuntimeException e) {
            log.warn("Unable to emit packet trough {}: {}", deviceId, e.explain());
        }
    }
}
