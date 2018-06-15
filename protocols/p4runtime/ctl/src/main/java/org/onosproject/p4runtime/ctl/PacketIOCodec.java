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

package org.onosproject.p4runtime.ctl;

import com.google.protobuf.ByteString;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiControlMetadataId;
import org.onosproject.net.pi.model.PiPacketOperationType;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiControlMetadata;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.slf4j.Logger;
import p4.config.v1.P4InfoOuterClass;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.p4runtime.ctl.P4InfoBrowser.NotFoundException;
import static org.slf4j.LoggerFactory.getLogger;
import static p4.v1.P4RuntimeOuterClass.PacketIn;
import static p4.v1.P4RuntimeOuterClass.PacketMetadata;
import static p4.v1.P4RuntimeOuterClass.PacketOut;

/**
 * Encoder of packet metadata, from ONOS Pi* format, to P4Runtime protobuf messages, and vice versa.
 */
final class PacketIOCodec {

    private static final Logger log = getLogger(PacketIOCodec.class);

    private static final String PACKET_OUT = "packet_out";

    private static final String PACKET_IN = "packet_in";

    // TODO: implement cache of encoded entities.

    private PacketIOCodec() {
        // hide.
    }

    /**
     * Returns a P4Runtime packet out protobuf message, encoded from the given PiPacketOperation for the given pipeconf.
     * If a PI packet metadata inside the PacketOperation cannot be encoded, it is skipped, hence the returned PacketOut
     * collection of metadatas might have different size than the input one.
     * <p>
     * Please check the log for an explanation of any error that might have occurred.
     *
     * @param packet   PI packet operation
     * @param pipeconf the pipeconf for the program on the switch
     * @return a P4Runtime packet out protobuf message
     * @throws NotFoundException if the browser can't find the packet_out in the given p4Info
     */
    static PacketOut encodePacketOut(PiPacketOperation packet, PiPipeconf pipeconf)
            throws NotFoundException {

        //Get the P4browser
        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        //Get the packet out controller packet metadata
        P4InfoOuterClass.ControllerPacketMetadata controllerControlMetadata =
                browser.controllerPacketMetadatas().getByName(PACKET_OUT);
        PacketOut.Builder packetOutBuilder = PacketOut.newBuilder();

        //outer controller packet metadata id
        int controllerControlMetadataId = controllerControlMetadata.getPreamble().getId();

        //Add all its metadata to the packet out
        packetOutBuilder.addAllMetadata(encodeControlMetadata(packet, browser, controllerControlMetadataId));

        //Set the packet out payload
        packetOutBuilder.setPayload(ByteString.copyFrom(packet.data().asReadOnlyBuffer()));
        return packetOutBuilder.build();

    }

    private static List<PacketMetadata> encodeControlMetadata(PiPacketOperation packet,
                                                              P4InfoBrowser browser, int controllerControlMetadataId) {
        return packet.metadatas().stream().map(metadata -> {
            try {
                //get each metadata id
                int metadataId = browser.packetMetadatas(controllerControlMetadataId)
                        .getByName(metadata.id().toString()).getId();

                //Add the metadata id and it's data the packet out
                return PacketMetadata.newBuilder()
                        .setMetadataId(metadataId)
                        .setValue(ByteString.copyFrom(metadata.value().asReadOnlyBuffer()))
                        .build();
            } catch (NotFoundException e) {
                log.error("Cant find metadata with name {} in p4Info file.", metadata.id());
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Returns a PiPacketOperation, decoded from the given P4Runtime PacketIn protobuf message for the given pipeconf
     * and device ID. If a PI packet metadata inside the protobuf message cannot be decoded, it is skipped, hence the
     * returned PiPacketOperation collection of metadatas might have different size than the input one.
     * <p>
     * Please check the log for an explanation of any error that might have occurred.
     *
     * @param packetIn the P4Runtime PacketIn message
     * @param pipeconf the pipeconf for the program on the switch
     * @param deviceId the deviceId that originated the PacketIn message
     * @return a PiPacketOperation
     */
    static PiPacketOperation decodePacketIn(PacketIn packetIn, PiPipeconf pipeconf, DeviceId deviceId) {

        //Get the P4browser
        P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        List<PiControlMetadata> packetMetadatas;
        try {
            int controllerControlMetadataId = browser.controllerPacketMetadatas().getByName(PACKET_IN)
                    .getPreamble().getId();
            packetMetadatas = decodeControlMetadataIn(packetIn.getMetadataList(), browser,
                                                      controllerControlMetadataId);
        } catch (NotFoundException e) {
            log.error("Unable to decode packet metadatas: {}", e.getMessage());
            packetMetadatas = Collections.emptyList();
        }

        //Transform the packetIn data
        ImmutableByteSequence data = copyFrom(packetIn.getPayload().asReadOnlyByteBuffer());

        //Build the PiPacketOperation with all the metadatas.
        return PiPacketOperation.builder()
                .forDevice(deviceId)
                .withType(PiPacketOperationType.PACKET_IN)
                .withMetadatas(packetMetadatas)
                .withData(data)
                .build();
    }

    private static List<PiControlMetadata> decodeControlMetadataIn(List<PacketMetadata> packetMetadatas,
                                                                   P4InfoBrowser browser,
                                                                   int controllerControlMetadataId) {
        return packetMetadatas.stream().map(packetMetadata -> {
            try {

                int packetMetadataId = packetMetadata.getMetadataId();
                String packetMetadataName = browser.packetMetadatas(controllerControlMetadataId)
                        .getById(packetMetadataId).getName();

                PiControlMetadataId metadataId = PiControlMetadataId.of(packetMetadataName);

                //Build each metadata.
                return PiControlMetadata.builder()
                        .withId(metadataId)
                        .withValue(ImmutableByteSequence.copyFrom(packetMetadata.getValue().asReadOnlyByteBuffer()))
                        .build();
            } catch (NotFoundException e) {
                log.error("Cant find metadata with id {} in p4Info file.", packetMetadata.getMetadataId());
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

}
