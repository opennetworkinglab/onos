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

import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiCloneSessionEntry;
import org.onosproject.net.pi.runtime.PiCloneSessionEntryHandle;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.v1.P4RuntimeOuterClass;

import static org.onosproject.p4runtime.ctl.codec.Codecs.CODECS;

/**
 * Codec for P4Runtime CloneSessionEntry.
 */
public final class CloneSessionEntryCodec
        extends AbstractEntityCodec<PiCloneSessionEntry, PiCloneSessionEntryHandle,
        P4RuntimeOuterClass.CloneSessionEntry, Object> {

    @Override
    protected P4RuntimeOuterClass.CloneSessionEntry encode(
            PiCloneSessionEntry piEntity, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser) throws CodecException {
        return P4RuntimeOuterClass.CloneSessionEntry.newBuilder()
                .setSessionId(piEntity.sessionId())
                .addAllReplicas(
                        CODECS.preReplica().encodeAll(
                                piEntity.replicas(), null, pipeconf))
                .setClassOfService(piEntity.classOfService())
                .setPacketLengthBytes(piEntity.maxPacketLengthBytes())
                .build();
    }

    @Override
    protected P4RuntimeOuterClass.CloneSessionEntry encodeKey(
            PiCloneSessionEntryHandle handle, Object metadata,
            PiPipeconf pipeconf, P4InfoBrowser browser) {
        return P4RuntimeOuterClass.CloneSessionEntry.newBuilder()
                .setSessionId(handle.sessionId()).build();
    }

    @Override
    protected P4RuntimeOuterClass.CloneSessionEntry encodeKey(
            PiCloneSessionEntry piEntity, Object metadata,
            PiPipeconf pipeconf, P4InfoBrowser browser) {
        return P4RuntimeOuterClass.CloneSessionEntry.newBuilder()
                .setSessionId(piEntity.sessionId()).build();
    }

    @Override
    protected PiCloneSessionEntry decode(
            P4RuntimeOuterClass.CloneSessionEntry message, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser) throws CodecException {
        return PiCloneSessionEntry.builder()
                .withSessionId(message.getSessionId())
                .addReplicas(
                        CODECS.preReplica().decodeAll(
                                message.getReplicasList(), null, pipeconf))
                .withClassOfService(message.getClassOfService())
                .withMaxPacketLengthBytes(message.getPacketLengthBytes())
                .build();
    }
}
