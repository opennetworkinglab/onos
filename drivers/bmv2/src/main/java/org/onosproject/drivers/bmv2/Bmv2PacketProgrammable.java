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

package org.onosproject.drivers.bmv2;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketProgrammable;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.runtime.PiPipeconfService;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Packet Programmable behaviour for BMv2 devices.
 */
public class Bmv2PacketProgrammable extends AbstractHandlerBehaviour implements PacketProgrammable {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void emit(OutboundPacket packet) {

        DeviceId deviceId = handler().data().deviceId();
        P4RuntimeController controller = handler().get(P4RuntimeController.class);
        if (!controller.hasClient(deviceId)) {
            log.warn("Unable to find client for {}, aborting the sending packet", deviceId);
            return;
        }

        P4RuntimeClient client = controller.getClient(deviceId);
        PiPipeconfService piPipeconfService = handler().get(PiPipeconfService.class);

        final PiPipeconf pipeconf;
        if (piPipeconfService.ofDevice(deviceId).isPresent() &&
                piPipeconfService.getPipeconf(piPipeconfService.ofDevice(deviceId).get()).isPresent()) {
            pipeconf = piPipeconfService.getPipeconf(piPipeconfService.ofDevice(deviceId).get()).get();
        } else {
            log.warn("Unable to get the pipeconf of the {}", deviceId);
            return;
        }

        DeviceService deviceService = handler().get(DeviceService.class);
        Device device = deviceService.getDevice(deviceId);
        final PiPipelineInterpreter interpreter = device.is(PiPipelineInterpreter.class)
                ? device.as(PiPipelineInterpreter.class) : null;
        if (interpreter == null) {
            log.warn("Device {} unable to instantiate interpreter of pipeconf {}", deviceId, pipeconf.id());
            return;
        }

        try {
            Collection<PiPacketOperation> operations = interpreter.mapOutboundPacket(packet, pipeconf);
            operations.forEach(piPacketOperation -> {
                client.packetOut(piPacketOperation, pipeconf);
            });
        } catch (PiPipelineInterpreter.PiInterpreterException e) {
            log.error("Interpreter of pipeconf {} was unable to translate outbound packet: {}",
                    pipeconf.id(), e.getMessage());
        }
    }
}
