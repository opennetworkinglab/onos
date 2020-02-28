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

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.flow.criteria.Criterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Criterion codec.
 */
public final class CriterionCodec extends JsonCodec<Criterion> {

    private static final Logger log =
            LoggerFactory.getLogger(CriterionCodec.class);

    static final String TYPE = "type";
    static final String ETH_TYPE = "ethType";
    static final String MAC = "mac";
    static final String MAC_MASK = "macMask";
    static final String PORT = "port";
    static final String METADATA = "metadata";

    static final String VLAN_ID = "vlanId";
    static final String INNER_VLAN_ID = "innerVlanId";
    static final String INNER_PRIORITY = "innerPriority";
    static final String PRIORITY = "priority";
    static final String IP_DSCP = "ipDscp";
    static final String IP_ECN = "ipEcn";
    static final String PROTOCOL = "protocol";
    static final String IP = "ip";
    static final String TCP_PORT = "tcpPort";
    static final String TCP_MASK = "tcpMask";
    static final String UDP_PORT = "udpPort";
    static final String UDP_MASK = "udpMask";
    static final String SCTP_PORT = "sctpPort";
    static final String SCTP_MASK = "sctpMask";
    static final String ICMP_TYPE = "icmpType";
    static final String ICMP_CODE = "icmpCode";
    static final String FLOW_LABEL = "flowLabel";
    static final String ICMPV6_TYPE = "icmpv6Type";
    static final String ICMPV6_CODE = "icmpv6Code";
    static final String TARGET_ADDRESS = "targetAddress";
    static final String LABEL = "label";
    static final String BOS = "bos";
    static final String EXT_HDR_FLAGS = "exthdrFlags";
    static final String LAMBDA = "lambda";
    static final String GRID_TYPE = "gridType";
    static final String CHANNEL_SPACING = "channelSpacing";
    static final String SPACING_MULIPLIER = "spacingMultiplier";
    static final String SLOT_GRANULARITY = "slotGranularity";
    static final String OCH_SIGNAL_ID = "ochSignalId";
    static final String TUNNEL_ID = "tunnelId";
    static final String OCH_SIGNAL_TYPE = "ochSignalType";
    static final String ODU_SIGNAL_ID = "oduSignalId";
    static final String TRIBUTARY_PORT_NUMBER = "tributaryPortNumber";
    static final String TRIBUTARY_SLOT_LEN = "tributarySlotLen";
    static final String TRIBUTARY_SLOT_BITMAP = "tributarySlotBitmap";
    static final String ODU_SIGNAL_TYPE = "oduSignalType";
    static final String PI_MATCHES = "matches";
    static final String PI_MATCH_FIELD_ID = "field";
    static final String PI_MATCH_TYPE = "match";
    static final String PI_MATCH_VALUE = "value";
    static final String PI_MATCH_PREFIX = "prefixLength";
    static final String PI_MATCH_MASK = "mask";
    static final String PI_MATCH_HIGH_VALUE = "highValue";
    static final String PI_MATCH_LOW_VALUE = "lowValue";
    static final String EXTENSION = "extension";

    @Override
    public ObjectNode encode(Criterion criterion, CodecContext context) {
        EncodeCriterionCodecHelper encoder = new EncodeCriterionCodecHelper(criterion, context);
        return encoder.encode();
    }

    @Override
    public Criterion decode(ObjectNode json, CodecContext context) {
        DecodeCriterionCodecHelper decoder = new DecodeCriterionCodecHelper(json);
        return decoder.decode();
    }
}
