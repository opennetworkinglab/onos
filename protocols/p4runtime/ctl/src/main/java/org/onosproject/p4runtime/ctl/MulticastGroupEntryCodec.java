/*
 * Copyright 2018-present Open Networking Foundation
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

import org.onosproject.net.PortNumber;
import org.onosproject.net.pi.runtime.PiMulticastGroupEntry;
import org.onosproject.net.pi.runtime.PiPreReplica;
import p4.v1.P4RuntimeOuterClass.MulticastGroupEntry;
import p4.v1.P4RuntimeOuterClass.Replica;

import static java.lang.String.format;

/**
 * A coded of {@link PiMulticastGroupEntry} to P4Runtime MulticastGroupEntry
 * messages, and vice versa.
 */
final class MulticastGroupEntryCodec {

    private MulticastGroupEntryCodec() {
        // Hides constructor.
    }

    /**
     * Returns a P4Runtime MulticastGroupEntry message equivalent to the given
     * PiMulticastGroupEntry.
     *
     * @param piEntry PiMulticastGroupEntry
     * @return P4Runtime MulticastGroupEntry message
     * @throws EncodeException if the PiMulticastGroupEntry cannot be encoded.
     */
    static MulticastGroupEntry encode(PiMulticastGroupEntry piEntry) throws EncodeException {
        final MulticastGroupEntry.Builder msgBuilder = MulticastGroupEntry.newBuilder();
        msgBuilder.setMulticastGroupId(piEntry.groupId());
        for (PiPreReplica replica : piEntry.replicas()) {
            final int p4PortId;
            try {
                p4PortId = Math.toIntExact(replica.egressPort().toLong());
            } catch (ArithmeticException e) {
                throw new EncodeException(format(
                        "Cannot cast 64bit port value '%s' to 32bit",
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

    /**
     * Returns a PiMulticastGroupEntry equivalent to the given P4Runtime
     * MulticastGroupEntry message.
     *
     * @param msg P4Runtime MulticastGroupEntry message
     * @return PiMulticastGroupEntry
     */
    static PiMulticastGroupEntry decode(MulticastGroupEntry msg) {
        final PiMulticastGroupEntry.Builder piEntryBuilder = PiMulticastGroupEntry.builder();
        piEntryBuilder.withGroupId(msg.getMulticastGroupId());
        msg.getReplicasList().stream()
                .map(r -> new PiPreReplica(
                        PortNumber.portNumber(r.getEgressPort()), r.getInstance()))
                .forEach(piEntryBuilder::addReplica);
        return piEntryBuilder.build();
    }
}
