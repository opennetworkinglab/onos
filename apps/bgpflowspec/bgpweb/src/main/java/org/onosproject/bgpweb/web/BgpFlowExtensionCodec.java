/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.bgpweb.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.flowapi.ExtFlowTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpFlowExtensionCodec extends JsonCodec<ExtFlowTypes> {

    protected static final Logger log =
            LoggerFactory.getLogger(BgpFlowExtensionCodec.class);

    protected static final String TYPE = "type";
    protected static final String NAME = "name";
    protected static final String PREFIX = "prefix";
    protected static final String PROTOCOLS = "protocols";
    protected static final String PORT = "port";
    protected static final String DST_PORT = "destinationPort";
    protected static final String SRC_PORT = "sourcePort";
    protected static final String ICMP_TYPE = "icmpType";
    protected static final String ICMP_CODE = "icmpCode";
    protected static final String TCP_FLAG = "tcpFlag";
    protected static final String PACKET_LENGTH = "packetLength";
    protected static final String DSCP_VALUE = "dscpValue";
    protected static final String FRAGMENT = "fragment";

    protected static final String TRAFFIC_RATE = "trafficRate";
    protected static final String TRAFFIC_ACTION = "trafficAction";
    protected static final String TRAFFIC_REDIRECTION = "trafficRedirection";
    protected static final String TRAFFIC_MARKING = "trafficMarking";

    protected static final String TRAFFIC_RATE_ASN = "asn";
    protected static final String TRAFFIC_RATE_RATE = "rate";
    protected static final String TRAFFIC_ACTION_TERMINAL = "terminal";
    protected static final String TRAFFIC_ACTION_SAMPLE = "sample";
    protected static final String TRAFFIC_ACTION_RPD = "rpd";

    protected static final String WIDE_COMM_FLAGS = "widecommunityFlags";
    protected static final String WIDE_COMM_HOP_COUNT = "widecommunityHopCount";
    protected static final String WIDE_COMM_COMMUNITY = "widecommunityCommunity";
    protected static final String WIDE_COMM_CONTEXT_AS = "widecommunityContextAs";
    protected static final String WIDE_COMM_LOCAL_AS = "widecommunityLocalAs";
    protected static final String WIDE_COMM_TARGET = "widecommunityTarget";
    protected static final String WIDE_COMM_EXT_TARGET = "widecommunityExtTarget";
    protected static final String WIDE_COMM_PARAMETER = "widecommunityParameter";

    protected static final String WIDE_COMM_TGT_LOCAL_SP = "localSpeaker";
    protected static final String WIDE_COMM_TGT_REMOTE_SP = "remoteSpeaker";

    @Override
    public ObjectNode encode(ExtFlowTypes flowTypes, CodecContext context) {
        return null;
    }

    @Override
    public ExtFlowTypes decode(ObjectNode json, CodecContext context) {
        DecodeBgpFlowExtnCodecHelper decoder = new DecodeBgpFlowExtnCodecHelper(json);
        return decoder.decode();
    }
}
