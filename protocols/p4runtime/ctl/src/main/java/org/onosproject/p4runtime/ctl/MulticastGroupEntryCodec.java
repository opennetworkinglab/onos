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
     */
    static MulticastGroupEntry encode(PiMulticastGroupEntry piEntry) {
        final MulticastGroupEntry.Builder msgBuilder = MulticastGroupEntry.newBuilder();
        msgBuilder.setMulticastGroupId(piEntry.groupId());
        piEntry.replicas().stream()
                .map(r -> Replica.newBuilder()
                        .setEgressPort(r.egressPort().toLong())
                        .setInstance(r.instanceId())
                        .build())
                .forEach(msgBuilder::addReplicas);
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
