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

/**
 * Utility class that provides access to P4Runtime codec instances.
 */
public final class Codecs {

    public static final Codecs CODECS = new Codecs();

    private final ActionCodec action;
    private final ActionProfileGroupCodec actionProfileGroup;
    private final ActionProfileMemberCodec actionProfileMember;
    private final CounterEntryCodec counterEntry;
    private final DirectCounterEntryCodec directCounterEntry;
    private final DirectMeterEntryCodec directMeterEntry;
    private final EntityCodec entity;
    private final FieldMatchCodec fieldMatch;
    private final HandleCodec handle;
    private final MeterEntryCodec meterEntry;
    private final MulticastGroupEntryCodec multicastGroupEntry;
    private final CloneSessionEntryCodec cloneSessionEntry;
    private final PreReplicaCodec preReplica;
    private final PacketInCodec packetIn;
    private final PacketMetadataCodec packetMetadata;
    private final PacketOutCodec packetOut;
    private final TableEntryCodec tableEntry;
    private final ActionSetCodec actionSet;

    private Codecs() {
        this.action = new ActionCodec();
        this.actionProfileGroup = new ActionProfileGroupCodec();
        this.actionProfileMember = new ActionProfileMemberCodec();
        this.counterEntry = new CounterEntryCodec();
        this.directCounterEntry = new DirectCounterEntryCodec();
        this.directMeterEntry = new DirectMeterEntryCodec();
        this.entity = new EntityCodec();
        this.fieldMatch = new FieldMatchCodec();
        this.handle = new HandleCodec();
        this.meterEntry = new MeterEntryCodec();
        this.multicastGroupEntry = new MulticastGroupEntryCodec();
        this.cloneSessionEntry = new CloneSessionEntryCodec();
        this.preReplica = new PreReplicaCodec();
        this.packetIn = new PacketInCodec();
        this.packetMetadata = new PacketMetadataCodec();
        this.packetOut = new PacketOutCodec();
        this.tableEntry = new TableEntryCodec();
        this.actionSet = new ActionSetCodec();
    }

    public EntityCodec entity() {
        return entity;
    }

    public HandleCodec handle() {
        return handle;
    }

    public PacketOutCodec packetOut() {
        return packetOut;
    }

    public PacketInCodec packetIn() {
        return packetIn;
    }

    TableEntryCodec tableEntry() {
        return tableEntry;
    }

    FieldMatchCodec fieldMatch() {
        return fieldMatch;
    }

    ActionCodec action() {
        return action;
    }

    ActionProfileMemberCodec actionProfileMember() {
        return actionProfileMember;
    }

    ActionProfileGroupCodec actionProfileGroup() {
        return actionProfileGroup;
    }

    PacketMetadataCodec packetMetadata() {
        return packetMetadata;
    }

    MulticastGroupEntryCodec multicastGroupEntry() {
        return multicastGroupEntry;
    }

    CloneSessionEntryCodec cloneSessionEntry() {
        return cloneSessionEntry;
    }

    PreReplicaCodec preReplica() {
        return preReplica;
    }

    DirectMeterEntryCodec directMeterEntry() {
        return directMeterEntry;
    }

    MeterEntryCodec meterEntry() {
        return meterEntry;
    }

    CounterEntryCodec counterEntry() {
        return counterEntry;
    }

    DirectCounterEntryCodec directCounterEntry() {
        return directCounterEntry;
    }

    ActionSetCodec actionSet() {
        return actionSet;
    }
}
