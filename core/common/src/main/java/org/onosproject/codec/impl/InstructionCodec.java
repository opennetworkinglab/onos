/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.flow.instructions.Instruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Instruction codec.
 */
public final class InstructionCodec extends JsonCodec<Instruction> {

    private static final Logger log = LoggerFactory.getLogger(InstructionCodec.class);

    static final String TYPE = "type";
    static final String SUBTYPE = "subtype";
    static final String PORT = "port";
    static final String MAC = "mac";
    static final String VLAN_ID = "vlanId";
    static final String VLAN_PCP = "vlanPcp";
    static final String MPLS_LABEL = "label";
    static final String MPLS_BOS = "bos";
    static final String IP = "ip";
    static final String IP_DSCP = "ipDscp";
    static final String FLOW_LABEL = "flowLabel";
    static final String LAMBDA = "lambda";
    static final String GRID_TYPE = "gridType";
    static final String CHANNEL_SPACING = "channelSpacing";
    static final String SPACING_MULTIPLIER = "spacingMultiplier";
    static final String SLOT_GRANULARITY = "slotGranularity";
    static final String ETHERNET_TYPE = "ethernetType";
    static final String TUNNEL_ID = "tunnelId";
    static final String TCP_PORT = "tcpPort";
    static final String UDP_PORT = "udpPort";
    static final String TABLE_ID = "tableId";
    static final String GROUP_ID = "groupId";
    static final String METER_ID = "meterId";
    static final String QUEUE_ID = "queueId";
    static final String TRIBUTARY_PORT_NUMBER = "tributaryPortNumber";
    static final String TRIBUTARY_SLOT_LEN = "tributarySlotLength";
    static final String TRIBUTARY_SLOT_BITMAP = "tributarySlotBitmap";
    static final String EXTENSION = "extension";
    static final String DEVICE_ID = "deviceId";
    static final String STAT_TRIGGER_FLAG = "statTriggerFlag";
    static final String STAT_THRESHOLDS = "statThreshold";
    static final String STAT_BYTE_COUNT = "byteCount";
    static final String STAT_PACKET_COUNT = "packetCount";
    static final String STAT_DURATION = "duration";

    static final String PI_ACTION_ID = "actionId";
    static final String PI_ACTION_PROFILE_GROUP_ID = "groupId";
    static final String PI_ACTION_PROFILE_MEMBER_ID = "memberId";
    static final String PI_ACTION_PARAMS = "actionParams";

    static final String MISSING_MEMBER_MESSAGE =
            " member is required in Instruction";
    static final String ERROR_MESSAGE =
            " not specified in Instruction";


    @Override
    public ObjectNode encode(Instruction instruction, CodecContext context) {
        checkNotNull(instruction, "Instruction cannot be null");

        return new EncodeInstructionCodecHelper(instruction, context).encode();
    }

    @Override
    public Instruction decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        return new DecodeInstructionCodecHelper(json, context).decode();
    }
}
