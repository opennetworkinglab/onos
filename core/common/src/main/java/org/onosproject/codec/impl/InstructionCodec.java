/*
 * Copyright 2015-present Open Networking Laboratory
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

    protected static final Logger log = LoggerFactory.getLogger(InstructionCodec.class);

    protected static final String TYPE = "type";
    protected static final String SUBTYPE = "subtype";
    protected static final String PORT = "port";
    protected static final String MAC = "mac";
    protected static final String VLAN_ID = "vlanId";
    protected static final String VLAN_PCP = "vlanPcp";
    protected static final String MPLS_LABEL = "label";
    protected static final String MPLS_BOS = "bos";
    protected static final String IP = "ip";
    protected static final String FLOW_LABEL = "flowLabel";
    protected static final String LAMBDA = "lambda";
    protected static final String GRID_TYPE = "gridType";
    protected static final String CHANNEL_SPACING = "channelSpacing";
    protected static final String SPACING_MULTIPLIER = "spacingMultiplier";
    protected static final String SLOT_GRANULARITY = "slotGranularity";
    protected static final String ETHERNET_TYPE = "ethernetType";
    protected static final String TUNNEL_ID = "tunnelId";
    protected static final String TCP_PORT = "tcpPort";
    protected static final String UDP_PORT = "udpPort";
    protected static final String TABLE_ID = "tableId";
    protected static final String GROUP_ID = "groupId";
    protected static final String METER_ID = "meterId";
    protected static final String QUEUE_ID = "queueId";
    protected static final String TRIBUTARY_PORT_NUMBER = "tributaryPortNumber";
    protected static final String TRIBUTARY_SLOT_LEN = "tributarySlotLength";
    protected static final String TRIBUTARY_SLOT_BITMAP = "tributarySlotBitmap";
    protected static final String EXTENSION = "extension";
    protected static final String DEVICE_ID = "deviceId";

    protected static final String MISSING_MEMBER_MESSAGE =
            " member is required in Instruction";


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
