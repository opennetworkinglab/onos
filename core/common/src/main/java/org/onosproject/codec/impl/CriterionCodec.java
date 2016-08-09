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

    protected static final Logger log =
            LoggerFactory.getLogger(CriterionCodec.class);

    protected static final String TYPE = "type";
    protected static final String ETH_TYPE = "ethType";
    protected static final String MAC = "mac";
    protected static final String PORT = "port";
    protected static final String METADATA = "metadata";

    protected static final String VLAN_ID = "vlanId";
    protected static final String INNER_VLAN_ID = "innerVlanId";
    protected static final String INNER_PRIORITY = "innerPriority";
    protected static final String PRIORITY = "priority";
    protected static final String IP_DSCP = "ipDscp";
    protected static final String IP_ECN = "ipEcn";
    protected static final String PROTOCOL = "protocol";
    protected static final String IP = "ip";
    protected static final String TCP_PORT = "tcpPort";
    protected static final String UDP_PORT = "udpPort";
    protected static final String SCTP_PORT = "sctpPort";
    protected static final String ICMP_TYPE = "icmpType";
    protected static final String ICMP_CODE = "icmpCode";
    protected static final String FLOW_LABEL = "flowLabel";
    protected static final String ICMPV6_TYPE = "icmpv6Type";
    protected static final String ICMPV6_CODE = "icmpv6Code";
    protected static final String TARGET_ADDRESS = "targetAddress";
    protected static final String LABEL = "label";
    protected static final String BOS = "bos";
    protected static final String EXT_HDR_FLAGS = "exthdrFlags";
    protected static final String LAMBDA = "lambda";
    protected static final String GRID_TYPE = "gridType";
    protected static final String CHANNEL_SPACING = "channelSpacing";
    protected static final String SPACING_MULIPLIER = "spacingMultiplier";
    protected static final String SLOT_GRANULARITY = "slotGranularity";
    protected static final String OCH_SIGNAL_ID = "ochSignalId";
    protected static final String TUNNEL_ID = "tunnelId";
    protected static final String OCH_SIGNAL_TYPE = "ochSignalType";
    protected static final String ODU_SIGNAL_ID = "oduSignalId";
    protected static final String TRIBUTARY_PORT_NUMBER = "tributaryPortNumber";
    protected static final String TRIBUTARY_SLOT_LEN = "tributarySlotLen";
    protected static final String TRIBUTARY_SLOT_BITMAP = "tributarySlotBitmap";
    protected static final String ODU_SIGNAL_TYPE = "oduSignalType";

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
