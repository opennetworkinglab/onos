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
import org.onosproject.net.pi.runtime.PiActionProfileGroupHandle;
import org.onosproject.net.pi.runtime.PiActionProfileMemberHandle;
import org.onosproject.net.pi.runtime.PiCloneSessionEntryHandle;
import org.onosproject.net.pi.runtime.PiCounterCellHandle;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.net.pi.runtime.PiMeterCellHandle;
import org.onosproject.net.pi.runtime.PiMulticastGroupEntryHandle;
import org.onosproject.net.pi.runtime.PiPreEntryHandle;
import org.onosproject.net.pi.runtime.PiTableEntryHandle;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.v1.P4RuntimeOuterClass;

import static java.lang.String.format;
import static org.onosproject.p4runtime.ctl.codec.Codecs.CODECS;

public final class HandleCodec extends AbstractCodec<PiHandle, P4RuntimeOuterClass.Entity, Object> {

    @Override
    protected P4RuntimeOuterClass.Entity encode(
            PiHandle piHandle, Object ignored, PiPipeconf pipeconf,
            P4InfoBrowser browser)
            throws CodecException {
        final P4RuntimeOuterClass.Entity.Builder p4Entity = P4RuntimeOuterClass.Entity.newBuilder();

        switch (piHandle.entityType()) {
            case TABLE_ENTRY:
                return p4Entity.setTableEntry(
                        CODECS.tableEntry().encodeKey(
                                (PiTableEntryHandle) piHandle, null, pipeconf))
                        .build();
            case ACTION_PROFILE_GROUP:
                return p4Entity.setActionProfileGroup(
                        CODECS.actionProfileGroup().encodeKey(
                                (PiActionProfileGroupHandle) piHandle, null, pipeconf))
                        .build();
            case ACTION_PROFILE_MEMBER:
                return p4Entity.setActionProfileMember(
                        CODECS.actionProfileMember().encodeKey(
                                (PiActionProfileMemberHandle) piHandle, null, pipeconf))
                        .build();
            case PRE_ENTRY:
                final PiPreEntryHandle preEntryHandle = (PiPreEntryHandle) piHandle;
                switch (preEntryHandle.preEntryType()) {
                    case MULTICAST_GROUP:
                        return p4Entity.setPacketReplicationEngineEntry(
                                P4RuntimeOuterClass.PacketReplicationEngineEntry.newBuilder()
                                        .setMulticastGroupEntry(CODECS.multicastGroupEntry().encodeKey(
                                                (PiMulticastGroupEntryHandle) piHandle, null, pipeconf))
                                        .build())
                                .build();
                    case CLONE_SESSION:
                        return p4Entity.setPacketReplicationEngineEntry(
                                P4RuntimeOuterClass.PacketReplicationEngineEntry.newBuilder()
                                        .setCloneSessionEntry(CODECS.cloneSessionEntry().encodeKey(
                                                (PiCloneSessionEntryHandle) piHandle, null, pipeconf))
                                        .build())
                                .build();
                    default:
                        throw new CodecException(format(
                                "Encoding of handle for %s of type %s is not supported",
                                piHandle.entityType(),
                                preEntryHandle.preEntryType()));
                }
            case METER_CELL_CONFIG:
                final PiMeterCellHandle meterCellHandle = (PiMeterCellHandle) piHandle;
                switch (meterCellHandle.cellId().meterType()) {
                    case DIRECT:
                        return p4Entity.setDirectMeterEntry(
                                CODECS.directMeterEntry().encodeKey(
                                        meterCellHandle, null, pipeconf))
                                .build();
                    case INDIRECT:
                        return p4Entity.setMeterEntry(
                                CODECS.meterEntry().encodeKey(
                                        meterCellHandle, null, pipeconf))
                                .build();
                    default:
                        throw new CodecException(format(
                                "Encoding of handle for %s of type %s is not supported",
                                piHandle.entityType(),
                                meterCellHandle.cellId().meterType()));
                }
            case COUNTER_CELL:
                final PiCounterCellHandle counterCellHandle = (PiCounterCellHandle) piHandle;
                switch (counterCellHandle.cellId().counterType()) {
                    case DIRECT:
                        return p4Entity.setDirectCounterEntry(
                                CODECS.directCounterEntry().encodeKey(
                                        counterCellHandle, null, pipeconf))
                                .build();
                    case INDIRECT:
                        return p4Entity.setCounterEntry(
                                CODECS.counterEntry().encodeKey(
                                        counterCellHandle, null, pipeconf))
                                .build();
                    default:
                        throw new CodecException(format(
                                "Encoding of handle for %s of type %s is not supported",
                                piHandle.entityType(),
                                counterCellHandle.cellId().counterType()));
                }
            case REGISTER_CELL:
            default:
                throw new CodecException(format(
                        "Encoding of handle for %s not supported",
                        piHandle.entityType()));
        }
    }

    @Override
    protected PiHandle decode(
            P4RuntimeOuterClass.Entity message, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException {
        throw new CodecException("Decoding of Entity to PiHandle is not supported");
    }
}
