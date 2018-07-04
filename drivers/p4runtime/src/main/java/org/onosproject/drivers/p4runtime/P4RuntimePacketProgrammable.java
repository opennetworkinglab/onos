/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.p4runtime;

import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketProgrammable;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.runtime.PiPacketOperation;

import java.util.Collection;

/**
 * Implementation of PacketProgrammable behaviour for P4Runtime.
 */
public class P4RuntimePacketProgrammable
        extends AbstractP4RuntimeHandlerBehaviour
        implements PacketProgrammable {

    @Override
    public void emit(OutboundPacket packet) {

        if (!this.setupBehaviour()) {
            return;
        }

        final PiPipelineInterpreter interpreter = getInterpreter();
        if (interpreter == null) {
            // Error logged by getInterpreter().
            return;
        }

        try {
            Collection<PiPacketOperation> operations = interpreter.mapOutboundPacket(packet);
            operations.forEach(piPacketOperation -> {
                log.debug("Doing PiPacketOperation {}", piPacketOperation);
                getFutureWithDeadline(
                        client.packetOut(piPacketOperation, pipeconf),
                        "sending packet-out", false);
            });
        } catch (PiPipelineInterpreter.PiInterpreterException e) {
            log.error("Unable to translate outbound packet for {} with pipeconf {}: {}",
                      deviceId, pipeconf.id(), e.getMessage());
        }
    }
}
