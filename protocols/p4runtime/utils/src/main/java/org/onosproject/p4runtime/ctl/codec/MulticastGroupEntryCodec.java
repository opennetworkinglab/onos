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
import org.onosproject.net.pi.runtime.PiMulticastGroupEntry;
import org.onosproject.net.pi.runtime.PiMulticastGroupEntryHandle;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.v1.P4RuntimeOuterClass;

import static org.onosproject.p4runtime.ctl.codec.Codecs.CODECS;

/**
 * Codec for P4Runtime MulticastGroupEntry.
 */
public final class MulticastGroupEntryCodec
        extends AbstractEntityCodec<PiMulticastGroupEntry, PiMulticastGroupEntryHandle,
        P4RuntimeOuterClass.MulticastGroupEntry, Object> {

    @Override
    protected P4RuntimeOuterClass.MulticastGroupEntry encode(
            PiMulticastGroupEntry piEntity, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser) throws CodecException {
        return P4RuntimeOuterClass.MulticastGroupEntry.newBuilder()
                .setMulticastGroupId(piEntity.groupId())
                .addAllReplicas(
                        CODECS.preReplica().encodeAll(
                                piEntity.replicas(), null, pipeconf))
                .build();
    }

    @Override
    protected P4RuntimeOuterClass.MulticastGroupEntry encodeKey(
            PiMulticastGroupEntryHandle handle, Object metadata,
            PiPipeconf pipeconf, P4InfoBrowser browser) {
        return P4RuntimeOuterClass.MulticastGroupEntry.newBuilder()
                .setMulticastGroupId(handle.groupId()).build();
    }

    @Override
    protected P4RuntimeOuterClass.MulticastGroupEntry encodeKey(
            PiMulticastGroupEntry piEntity, Object metadata,
            PiPipeconf pipeconf, P4InfoBrowser browser) {
        return P4RuntimeOuterClass.MulticastGroupEntry.newBuilder()
                .setMulticastGroupId(piEntity.groupId()).build();
    }

    @Override
    protected PiMulticastGroupEntry decode(
            P4RuntimeOuterClass.MulticastGroupEntry message, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser) throws CodecException {
        return PiMulticastGroupEntry.builder()
                .withGroupId(message.getMulticastGroupId())
                .addReplicas(
                        CODECS.preReplica().decodeAll(
                                message.getReplicasList(), null, pipeconf))
                .build();
    }
}
