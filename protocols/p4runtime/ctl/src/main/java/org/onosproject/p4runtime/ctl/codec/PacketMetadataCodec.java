/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.p4runtime.ctl.codec;

import com.google.protobuf.ByteString;
import org.onosproject.net.pi.model.PiPacketMetadataId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiPacketMetadata;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.config.v1.P4InfoOuterClass;
import p4.v1.P4RuntimeOuterClass;

import static org.onlab.util.ImmutableByteSequence.copyFrom;

/**
 * Coded for P4Runtime PacketMetadata. The metadata is expected to be a Preamble
 * of a P4Info.ControllerPacketMetadata message.
 */
public final class PacketMetadataCodec
        extends AbstractCodec<PiPacketMetadata,
        P4RuntimeOuterClass.PacketMetadata, P4InfoOuterClass.Preamble> {

    @Override
    protected P4RuntimeOuterClass.PacketMetadata encode(
            PiPacketMetadata piEntity, P4InfoOuterClass.Preamble ctrlPktMetaPreamble,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        final int metadataId = browser
                .packetMetadatas(ctrlPktMetaPreamble.getId())
                .getByName(piEntity.id().id()).getId();
        return P4RuntimeOuterClass.PacketMetadata.newBuilder()
                .setMetadataId(metadataId)
                .setValue(ByteString.copyFrom(piEntity.value().asReadOnlyBuffer()))
                .build();
    }

    @Override
    protected PiPacketMetadata decode(
            P4RuntimeOuterClass.PacketMetadata message,
            P4InfoOuterClass.Preamble ctrlPktMetaPreamble,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        final String packetMetadataName = browser
                .packetMetadatas(ctrlPktMetaPreamble.getId())
                .getById(message.getMetadataId()).getName();
        final PiPacketMetadataId metadataId = PiPacketMetadataId
                .of(packetMetadataName);
        return PiPacketMetadata.builder()
                .withId(metadataId)
                .withValue(copyFrom(message.getValue().asReadOnlyByteBuffer()))
                .build();
    }
}
