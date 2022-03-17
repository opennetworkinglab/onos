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
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiPacketMetadataId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiPacketMetadata;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.config.v1.P4InfoOuterClass;
import p4.v1.P4RuntimeOuterClass;

import java.nio.ByteBuffer;

import static org.onlab.util.ImmutableByteSequence.copyAndFit;
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
        P4InfoOuterClass.ControllerPacketMetadata.Metadata packetMetadata = browser
                .packetMetadatas(ctrlPktMetaPreamble.getId())
                .getByName(piEntity.id().id());
        final ByteBuffer value;
        if (browser.isTypeString(packetMetadata.getTypeName())) {
            value = piEntity.value().asReadOnlyBuffer();
        } else {
            value = piEntity.value().canonical().asReadOnlyBuffer();
        }
        return P4RuntimeOuterClass.PacketMetadata.newBuilder()
                .setMetadataId(packetMetadata.getId())
                .setValue(ByteString.copyFrom(value))
                .build();
    }

    @Override
    protected PiPacketMetadata decode(
            P4RuntimeOuterClass.PacketMetadata message,
            P4InfoOuterClass.Preamble ctrlPktMetaPreamble,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, CodecException {
        final P4InfoOuterClass.ControllerPacketMetadata.Metadata pktMeta =
                browser.packetMetadatas(ctrlPktMetaPreamble.getId())
                        .getById(message.getMetadataId());
        final ImmutableByteSequence value;
        if (browser.isTypeString(pktMeta.getTypeName())) {
            value = copyFrom(new String(message.getValue().toByteArray()));
        } else {
            try {
                value = copyAndFit(message.getValue().asReadOnlyByteBuffer(),
                                   pktMeta.getBitwidth());
            } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
                throw new CodecException(e.getMessage());
            }
        }
        return PiPacketMetadata.builder()
                .withId(PiPacketMetadataId.of(pktMeta.getName()))
                .withValue(value)
                .build();
    }
}
