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
package org.onosproject.openstacktelemetry.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.onlab.packet.IPv4;
import org.onlab.packet.TpPort;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstacktelemetry.api.StatsFlowRule;
import org.onosproject.openstacktelemetry.impl.DefaultStatsFlowRule;
import org.slf4j.Logger;

import org.onlab.packet.IpPrefix;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

public class StatsFlowRuleJsonCodec extends JsonCodec<StatsFlowRule> {

    private final Logger log = getLogger(getClass());

    public static final String SRC_IP_PREFIX = "srcIpPrefix";
    public static final String DST_IP_PREFIX = "dstIpPrefix";
    public static final String IP_PROTOCOL   = "ipProtocol";
    public static final String SRC_TP_PORT   = "srcTpPort";
    public static final String DST_TP_PORT   = "dstTpPort";

    public ObjectNode encode(StatsFlowRule flowRule, CodecContext context) {
        checkNotNull(flowRule, "FlowInfo cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                            .put(SRC_IP_PREFIX, flowRule.srcIpPrefix().toString())
                            .put(DST_IP_PREFIX, flowRule.dstIpPrefix().toString())
                            .put(IP_PROTOCOL, flowRule.ipProtocol())
                            .put(SRC_TP_PORT, flowRule.srcTpPort().toString())
                            .put(DST_TP_PORT, flowRule.dstTpPort().toString());
        return result;
    }

    @Override
    public StatsFlowRule decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }
        try {
            String srcIpPrefix = json.get(SRC_IP_PREFIX).asText();
            String dstIpPrefix = json.get(DST_IP_PREFIX).asText();
            String tmpIpProtocol = new String("");
            int srcTpPort  = 0;
            int dstTpPort  = 0;

            DefaultStatsFlowRule.Builder flowRuleBuilder;

            byte ipProtocol = 0;
            if (json.get(IP_PROTOCOL) == null) {
                log.info("ipProtocol: null");
                flowRuleBuilder = DefaultStatsFlowRule.builder()
                        .srcIpPrefix(IpPrefix.valueOf(srcIpPrefix))
                        .dstIpPrefix(IpPrefix.valueOf(dstIpPrefix));
            } else {
                tmpIpProtocol = json.get(IP_PROTOCOL).asText().toUpperCase();
                srcTpPort     = json.get(SRC_TP_PORT).asInt();
                dstTpPort     = json.get(DST_TP_PORT).asInt();
                if (tmpIpProtocol.equals("TCP")) {
                    ipProtocol = IPv4.PROTOCOL_TCP;
                } else if (tmpIpProtocol.equals("UDP")) {
                    ipProtocol = IPv4.PROTOCOL_UDP;
                } else {
                    ipProtocol = 0;
                }

                flowRuleBuilder = DefaultStatsFlowRule.builder()
                        .srcIpPrefix(IpPrefix.valueOf(srcIpPrefix))
                        .dstIpPrefix(IpPrefix.valueOf(dstIpPrefix))
                        .ipProtocol(ipProtocol)
                        .srcTpPort(TpPort.tpPort(srcTpPort))
                        .dstTpPort(TpPort.tpPort(dstTpPort));
            }

            log.debug("StatsFlowRule after building from JSON:\n{}",
                     flowRuleBuilder.build().toString());

            return flowRuleBuilder.build();
        } catch (Exception ex) {
            log.error("Exception Stack:\n{}", ExceptionUtils.getStackTrace(ex));
        }
        return null;
    }
}
