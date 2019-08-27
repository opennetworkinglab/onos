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

import org.onlab.packet.EthType;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketProgrammable;
import org.onosproject.net.pi.model.PiPipelineInterpreter;

import java.nio.ByteBuffer;
import java.util.stream.Collectors;

import static org.onosproject.drivers.p4runtime.P4RuntimeDriverUtils.getInterpreter;
import static org.onosproject.net.flow.instructions.Instruction.Type.OUTPUT;

/**
 * Implementation of PacketProgrammable behaviour for P4Runtime.
 */
public class P4RuntimePacketProgrammable
        extends AbstractP4RuntimeHandlerBehaviour
        implements PacketProgrammable {

    @Override
    public void emit(OutboundPacket packet) {

        if (!this.setupBehaviour("emit()")) {
            return;
        }

        final PiPipelineInterpreter interpreter = getInterpreter(handler());
        if (interpreter == null) {
            // Error logged by getInterpreter().
            return;
        }

        if (log.isTraceEnabled()) {
            logPacketOut(packet);
        }

        try {
            interpreter.mapOutboundPacket(packet).forEach(
                    op -> client.packetOut(p4DeviceId, op, pipeconf));
        } catch (PiPipelineInterpreter.PiInterpreterException e) {
            log.error("Unable to translate outbound packet for {} with pipeconf {}: {}",
                      deviceId, pipeconf.id(), e.getMessage());
        }
    }

    private void logPacketOut(OutboundPacket packet) {
        final EthType.EtherType etherType = getEtherType(packet.data());
        final String outPorts = packet.treatment().immediate().stream()
                .filter(i -> i.type().equals(OUTPUT))
                .map(i -> Long.toString(((Instructions.OutputInstruction) i).port().toLong()))
                .collect(Collectors.joining(","));
        final String desc = outPorts.isBlank()
                ? "treatment=" + packet.treatment().toString()
                : "egress_ports=" + outPorts;
        log.trace("Sending PACKET-OUT >>> device={} {} eth_type={}",
                  packet.sendThrough(), desc,
                  etherType.ethType().toString());
    }

    private EthType.EtherType getEtherType(ByteBuffer data) {
        final short shortEthType = data.getShort(12);
        data.rewind();
        return EthType.EtherType.lookup(shortEthType);
    }
}
