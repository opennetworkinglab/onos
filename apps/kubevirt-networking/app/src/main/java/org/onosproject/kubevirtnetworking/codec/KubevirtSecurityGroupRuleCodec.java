/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpPrefix;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtSecurityGroupRule;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupRule;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubevirt security group rule codec used for serializing and de-serializing JSON string.
 */
public final class KubevirtSecurityGroupRuleCodec extends JsonCodec<KubevirtSecurityGroupRule> {

    private final Logger log = getLogger(getClass());

    private static final String ID = "id";
    private static final String SECURITY_GROUP_ID = "securityGroupId";
    private static final String DIRECTION = "direction";
    private static final String ETHER_TYPE = "etherType";
    private static final String PORT_RANGE_MAX = "portRangeMax";
    private static final String PORT_RANGE_MIN = "portRangeMin";
    private static final String PROTOCOL = "protocol";
    private static final String REMOTE_IP_PREFIX = "remoteIpPrefix";
    private static final String REMOTE_GROUP_ID = "remoteGroupId";

    private static final String MISSING_MESSAGE = " is required in KubevirtSecurityGroupRule";

    @Override
    public ObjectNode encode(KubevirtSecurityGroupRule sgRule, CodecContext context) {
        checkNotNull(sgRule, "Kubevirt security group rule cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(ID, sgRule.id())
                .put(SECURITY_GROUP_ID, sgRule.securityGroupId())
                .put(DIRECTION, sgRule.direction());

        if (sgRule.etherType() != null) {
            result.put(ETHER_TYPE, sgRule.etherType());
        }

        if (sgRule.portRangeMax() != null) {
            result.put(PORT_RANGE_MAX, sgRule.portRangeMax());
        }

        if (sgRule.portRangeMin() != null) {
            result.put(PORT_RANGE_MIN, sgRule.portRangeMin());
        }

        if (sgRule.protocol() != null) {
            result.put(PROTOCOL, sgRule.protocol());
        }

        if (sgRule.remoteIpPrefix() != null) {
            result.put(REMOTE_IP_PREFIX, sgRule.remoteIpPrefix().toString());
        }

        if (sgRule.remoteGroupId() != null) {
            result.put(REMOTE_GROUP_ID, sgRule.remoteGroupId());
        }

        return result;
    }

    @Override
    public KubevirtSecurityGroupRule decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String id = nullIsIllegal(json.get(ID).asText(), ID + MISSING_MESSAGE);
        String securityGroupId = nullIsIllegal(json.get(SECURITY_GROUP_ID).asText(),
                SECURITY_GROUP_ID + MISSING_MESSAGE);
        String direction = nullIsIllegal(json.get(DIRECTION).asText(),
                DIRECTION + MISSING_MESSAGE);

        KubevirtSecurityGroupRule.Builder builder = DefaultKubevirtSecurityGroupRule.builder()
                .id(id)
                .securityGroupId(securityGroupId)
                .direction(direction);

        JsonNode etherType = json.get(ETHER_TYPE);
        if (etherType != null) {
            builder.etherType(etherType.asText());
        }

        JsonNode portRangeMax = json.get(PORT_RANGE_MAX);
        if (portRangeMax != null) {
            builder.portRangeMax(portRangeMax.asInt());
        }

        JsonNode portRangeMin = json.get(PORT_RANGE_MIN);
        if (portRangeMin != null) {
            builder.portRangeMin(portRangeMin.asInt());
        }

        JsonNode protocol = json.get(PROTOCOL);
        if (protocol != null) {
            builder.protocol(protocol.asText());
        }

        JsonNode remoteIpPrefix = json.get(REMOTE_IP_PREFIX);
        if (remoteIpPrefix != null) {
            builder.remoteIpPrefix(IpPrefix.valueOf(remoteIpPrefix.asText()));
        }

        JsonNode remoteGroupId = json.get(REMOTE_GROUP_ID);
        if (remoteGroupId != null) {
            builder.remoteGroupId(remoteGroupId.asText());
        }

        return builder.build();
    }
}
