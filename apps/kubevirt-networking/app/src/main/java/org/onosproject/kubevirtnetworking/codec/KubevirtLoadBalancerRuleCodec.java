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
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtLoadBalancerRule;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerRule;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubevirt load balancer rule codec used for serializing and de-serializing JSON string.
 */
public final class KubevirtLoadBalancerRuleCodec extends JsonCodec<KubevirtLoadBalancerRule> {

    private final Logger log = getLogger(getClass());

    private static final String PROTOCOL = "protocol";
    private static final String PORT_RANGE_MAX = "portRangeMax";
    private static final String PORT_RANGE_MIN = "portRangeMin";
    private static final String TCP = "TCP";
    private static final String UDP = "UDP";

    private static final String MISSING_MESSAGE = " is required in KubevirtLoadBalancerRule";

    @Override
    public ObjectNode encode(KubevirtLoadBalancerRule rule, CodecContext context) {
        checkNotNull(rule, "Kubevirt load balancer rule cannot be null");

        ObjectNode result = context.mapper().createObjectNode().put(PROTOCOL, rule.protocol());

        if (rule.protocol().equalsIgnoreCase(TCP) || rule.protocol().equalsIgnoreCase(UDP)) {
            result.put(PORT_RANGE_MAX, rule.portRangeMax()).put(PORT_RANGE_MIN, rule.portRangeMin());
        }
        return result;
    }

    @Override
    public KubevirtLoadBalancerRule decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        KubevirtLoadBalancerRule.Builder builder = DefaultKubevirtLoadBalancerRule.builder();

        JsonNode protocolJson = json.get(PROTOCOL);
        String protocol = "";
        if (protocolJson != null) {
            protocol = protocolJson.asText();
        }
        builder.protocol(protocol);

        if (protocol.equalsIgnoreCase(TCP) || protocol.equalsIgnoreCase(UDP)) {
            Integer portRangeMax = json.get(PORT_RANGE_MAX).asInt();
            Integer portRangeMin = json.get(PORT_RANGE_MIN).asInt();
            builder.portRangeMax(portRangeMax).portRangeMin(portRangeMin);
        }

        return builder.build();
    }
}
