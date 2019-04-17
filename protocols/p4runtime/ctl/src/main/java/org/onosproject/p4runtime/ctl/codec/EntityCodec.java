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
import org.onosproject.net.pi.runtime.PiActionProfileGroup;
import org.onosproject.net.pi.runtime.PiActionProfileMember;
import org.onosproject.net.pi.runtime.PiCloneSessionEntry;
import org.onosproject.net.pi.runtime.PiCounterCell;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiMulticastGroupEntry;
import org.onosproject.net.pi.runtime.PiPreEntry;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.v1.P4RuntimeOuterClass;

import static java.lang.String.format;
import static org.onosproject.p4runtime.ctl.codec.Codecs.CODECS;

/**
 * Codec for P4Runtime Entity.
 */
public final class EntityCodec extends AbstractCodec<PiEntity, P4RuntimeOuterClass.Entity, Object> {

    @Override
    protected P4RuntimeOuterClass.Entity encode(
            PiEntity piEntity, Object ignored, PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException {
        final P4RuntimeOuterClass.Entity.Builder p4Entity = P4RuntimeOuterClass.Entity.newBuilder();
        switch (piEntity.piEntityType()) {
            case TABLE_ENTRY:
                return p4Entity.setTableEntry(
                        CODECS.tableEntry().encode(
                                (PiTableEntry) piEntity, null, pipeconf))
                        .build();
            case ACTION_PROFILE_GROUP:
                return p4Entity.setActionProfileGroup(
                        CODECS.actionProfileGroup().encode(
                                (PiActionProfileGroup) piEntity, null, pipeconf))
                        .build();
            case ACTION_PROFILE_MEMBER:
                return p4Entity.setActionProfileMember(
                        CODECS.actionProfileMember().encode(
                                (PiActionProfileMember) piEntity, null, pipeconf))
                        .build();
            case PRE_ENTRY:
                final PiPreEntry preEntry = (PiPreEntry) piEntity;
                switch (preEntry.preEntryType()) {
                    case MULTICAST_GROUP:
                        return p4Entity.setPacketReplicationEngineEntry(
                                P4RuntimeOuterClass.PacketReplicationEngineEntry.newBuilder()
                                        .setMulticastGroupEntry(CODECS.multicastGroupEntry().encode(
                                                (PiMulticastGroupEntry) piEntity, null, pipeconf))
                                        .build())
                                .build();
                    case CLONE_SESSION:
                        return p4Entity.setPacketReplicationEngineEntry(
                                P4RuntimeOuterClass.PacketReplicationEngineEntry.newBuilder()
                                        .setCloneSessionEntry(CODECS.cloneSessionEntry().encode(
                                                (PiCloneSessionEntry) piEntity, null, pipeconf))
                                        .build())
                                .build();
                    default:
                        throw new CodecException(format(
                                "Encoding of %s of type %s is not supported",
                                piEntity.piEntityType(),
                                preEntry.preEntryType()));
                }
            case METER_CELL_CONFIG:
                final PiMeterCellConfig meterCellConfig = (PiMeterCellConfig) piEntity;
                switch (meterCellConfig.cellId().meterType()) {
                    case DIRECT:
                        return p4Entity.setDirectMeterEntry(
                                CODECS.directMeterEntry().encode(
                                        meterCellConfig, null, pipeconf))
                                .build();
                    case INDIRECT:
                        return p4Entity.setMeterEntry(
                                CODECS.meterEntry().encode(
                                        meterCellConfig, null, pipeconf))
                                .build();
                    default:
                        throw new CodecException(format(
                                "Encoding of %s of type %s is not supported",
                                piEntity.piEntityType(),
                                meterCellConfig.cellId().meterType()));
                }
            case COUNTER_CELL:
                final PiCounterCell counterCell = (PiCounterCell) piEntity;
                switch (counterCell.cellId().counterType()) {
                    case DIRECT:
                        return p4Entity.setDirectCounterEntry(
                                CODECS.directCounterEntry().encode(
                                        counterCell, null, pipeconf))
                                .build();
                    case INDIRECT:
                        return p4Entity.setCounterEntry(
                                CODECS.counterEntry().encode(
                                        counterCell, null, pipeconf))
                                .build();
                    default:
                        throw new CodecException(format(
                                "Encoding of %s of type %s is not supported",
                                piEntity.piEntityType(),
                                counterCell.cellId().counterType()));
                }
            case REGISTER_CELL:
            default:
                throw new CodecException(format(
                        "Encoding of %s not supported",
                        piEntity.piEntityType()));
        }
    }

    @Override
    protected PiEntity decode(
            P4RuntimeOuterClass.Entity message, Object ignored, PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException {
        switch (message.getEntityCase()) {
            case TABLE_ENTRY:
                return CODECS.tableEntry().decode(
                        message.getTableEntry(), null, pipeconf);
            case ACTION_PROFILE_MEMBER:
                return CODECS.actionProfileMember().decode(
                        message.getActionProfileMember(), null, pipeconf);
            case ACTION_PROFILE_GROUP:
                return CODECS.actionProfileGroup().decode(
                        message.getActionProfileGroup(), null, pipeconf);
            case METER_ENTRY:
                return CODECS.meterEntry().decode(
                        message.getMeterEntry(), null, pipeconf);
            case DIRECT_METER_ENTRY:
                return CODECS.directMeterEntry().decode(
                        message.getDirectMeterEntry(), null, pipeconf);
            case COUNTER_ENTRY:
                return CODECS.counterEntry().decode(
                        message.getCounterEntry(), null, pipeconf);
            case DIRECT_COUNTER_ENTRY:
                return CODECS.directCounterEntry().decode(
                        message.getDirectCounterEntry(), null, pipeconf);
            case PACKET_REPLICATION_ENGINE_ENTRY:
                switch (message.getPacketReplicationEngineEntry().getTypeCase()) {
                    case MULTICAST_GROUP_ENTRY:
                        return CODECS.multicastGroupEntry().decode(
                                message.getPacketReplicationEngineEntry()
                                        .getMulticastGroupEntry(), null, pipeconf);
                    case CLONE_SESSION_ENTRY:
                        return CODECS.cloneSessionEntry().decode(
                                message.getPacketReplicationEngineEntry()
                                        .getCloneSessionEntry(), null, pipeconf);
                    case TYPE_NOT_SET:
                    default:
                        throw new CodecException(format(
                                "Decoding of %s of type %s not supported",
                                message.getEntityCase(),
                                message.getPacketReplicationEngineEntry().getTypeCase()));
                }
            case VALUE_SET_ENTRY:
            case REGISTER_ENTRY:
            case DIGEST_ENTRY:
            case EXTERN_ENTRY:
            case ENTITY_NOT_SET:
            default:
                throw new CodecException(format(
                        "Decoding of %s not supported",
                        message.getEntityCase()));

        }
    }
}
