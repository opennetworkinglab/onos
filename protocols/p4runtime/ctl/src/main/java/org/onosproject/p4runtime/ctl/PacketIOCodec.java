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

package org.onosproject.p4runtime.ctl;

import com.google.protobuf.ByteString;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.slf4j.Logger;
import p4.P4RuntimeOuterClass;
import p4.config.P4InfoOuterClass;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.onosproject.p4runtime.ctl.P4InfoBrowser.*;
import static org.slf4j.LoggerFactory.getLogger;
import static p4.P4RuntimeOuterClass.PacketMetadata;

/**
 * Encoder of packet metadata, from ONOS Pi* format, to P4Runtime protobuf messages, and vice versa.
 */
final class PacketIOCodec {

    private static final Logger log = getLogger(PacketIOCodec.class);

    private static final String PACKET_OUT = "packet_out";

    // TODO: implement cache of encoded entities.

    private PacketIOCodec() {
        // hide.
    }

    /**
     * Returns a P4Runtime packet out protobuf message, encoded from the given PiPacketOperation
     * for the given pipeconf. If a PI packet metadata inside the PacketOperation cannot be encoded,
     * it is skipped, hence the returned PacketOut collection of metadatas might have different
     * size than the input one.
     * <p>
     * Please check the log for an explanation of any error that might have occurred.
     *
     * @param packet   PI pakcet operation
     * @param pipeconf the pipeconf for the program on the switch
     * @return a P4Runtime packet out protobuf message
     * @throws NotFoundException if the browser can't find the packet_out in the given p4Info
     */
    static P4RuntimeOuterClass.PacketOut encodePacketOut(PiPacketOperation packet, PiPipeconf pipeconf)
            throws NotFoundException {

        //Get the P4browser
        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        //Get the packet out packet metadata
        P4InfoOuterClass.ControllerPacketMetadata controllerPacketMetadata =
                browser.controllerPacketMetadatas().getByName(PACKET_OUT);
        P4RuntimeOuterClass.PacketOut.Builder packetOutBuilder = P4RuntimeOuterClass.PacketOut.newBuilder();

        //outer controller packet metadata id
        int controllerPacketMetadataId = controllerPacketMetadata.getPreamble().getId();

        //Add all its metadata to the packet out
        packetOutBuilder.addAllMetadata(encodePacketMetadata(packet, browser, controllerPacketMetadataId));

        //Set the packet out payload
        packetOutBuilder.setPayload(ByteString.copyFrom(packet.data().asReadOnlyBuffer()));
        return packetOutBuilder.build();

    }

    private static List<PacketMetadata> encodePacketMetadata(PiPacketOperation packet,
                                                             P4InfoBrowser browser, int controllerPacketMetadataId) {
        return packet.metadatas().stream().map(metadata -> {
            try {
                //get each metadata id
                int metadataId = browser.packetMetadatas(controllerPacketMetadataId)
                        .getByName(metadata.id().name()).getId();

                //Add the metadata id and it's data the packet out
                return PacketMetadata.newBuilder()
                        .setMetadataId(metadataId)
                        .setValue(ByteString.copyFrom(metadata.value().asReadOnlyBuffer()))
                        .build();
            } catch (NotFoundException e) {
                log.error("Cant find metadata with name {} in p4Info file.", metadata.id().name());
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    //TODO: add decode packets

}
