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

import org.onosproject.net.PortNumber;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiMulticastGroupEntry;
import org.onosproject.net.pi.runtime.PiMulticastGroupEntryHandle;
import org.onosproject.net.pi.runtime.PiPreReplica;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.v1.P4RuntimeOuterClass;
import p4.v1.P4RuntimeOuterClass.Replica;

import static java.lang.String.format;

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
        final P4RuntimeOuterClass.MulticastGroupEntry.Builder msgBuilder =
                P4RuntimeOuterClass.MulticastGroupEntry.newBuilder()
                        .setMulticastGroupId(piEntity.groupId());
        for (PiPreReplica replica : piEntity.replicas()) {
            final int p4PortId;
            try {
                p4PortId = Math.toIntExact(replica.egressPort().toLong());
            } catch (ArithmeticException e) {
                throw new CodecException(format(
                        "Cannot cast 64 bit port value '%s' to 32 bit",
                        replica.egressPort()));
            }
            msgBuilder.addReplicas(
                    Replica.newBuilder()
                            .setEgressPort(p4PortId)
                            .setInstance(replica.instanceId())
                            .build());
        }
        return msgBuilder.build();
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
            PiPipeconf pipeconf, P4InfoBrowser browser) {
        final PiMulticastGroupEntry.Builder piEntryBuilder = PiMulticastGroupEntry.builder();
        piEntryBuilder.withGroupId(message.getMulticastGroupId());
        message.getReplicasList().stream()
                .map(r -> new PiPreReplica(
                        PortNumber.portNumber(r.getEgressPort()), r.getInstance()))
                .forEach(piEntryBuilder::addReplica);
        return piEntryBuilder.build();
    }
}
