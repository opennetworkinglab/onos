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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerRule;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

public final class KubevirtLoadBalancerCodec extends JsonCodec<KubevirtLoadBalancer> {

    private final Logger log = getLogger(getClass());

    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String VIP = "vip";
    private static final String NETWORK_ID = "networkId";
    private static final String MEMBERS = "members";
    private static final String RULES = "rules";

    private static final String MISSING_MESSAGE = " is required in KubevirtLoadBalancer";

    @Override
    public ObjectNode encode(KubevirtLoadBalancer lb, CodecContext context) {
        checkNotNull(lb, "Kubevirt load balancer cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(NAME, lb.name())
                .put(VIP, lb.vip().toString())
                .put(NETWORK_ID, lb.networkId());

        if (lb.description() != null) {
            result.put(DESCRIPTION, lb.description());
        }

        if (lb.members() != null && !lb.members().isEmpty()) {
            ArrayNode members = context.mapper().createArrayNode();
            for (IpAddress ip : lb.members()) {
                members.add(ip.toString());
            }
            result.set(MEMBERS, members);
        }

        if (lb.rules() != null && !lb.rules().isEmpty()) {
            ArrayNode rules = context.mapper().createArrayNode();
            for (KubevirtLoadBalancerRule rule : lb.rules()) {
                ObjectNode ruleJson = context.codec(
                        KubevirtLoadBalancerRule.class).encode(rule, context);
                rules.add(ruleJson);
            }
            result.set(RULES, rules);
        }

        return result;
    }

    @Override
    public KubevirtLoadBalancer decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String name = nullIsIllegal(json.get(NAME).asText(), NAME + MISSING_MESSAGE);
        IpAddress vip = IpAddress.valueOf(nullIsIllegal(json.get(VIP).asText(),
                VIP + MISSING_MESSAGE));
        String networkId = nullIsIllegal(json.get(NETWORK_ID).asText(),
                NETWORK_ID + MISSING_MESSAGE);

        KubevirtLoadBalancer.Builder builder = DefaultKubevirtLoadBalancer.builder()
                .name(name)
                .vip(vip)
                .networkId(networkId);

        JsonNode description = json.get(DESCRIPTION);
        if (description != null) {
            builder.description(description.asText());
        }

        ArrayNode membersJson = (ArrayNode) json.get(MEMBERS);
        if (membersJson != null) {
            Set<IpAddress> members = new HashSet<>();
            for (JsonNode memberJson : membersJson) {
                members.add(IpAddress.valueOf(memberJson.asText()));
            }
            builder.members(members);
        }

        JsonNode rulesJson = json.get(RULES);
        if (rulesJson != null) {
            Set<KubevirtLoadBalancerRule> rules = new HashSet<>();
            IntStream.range(0, rulesJson.size())
                    .forEach(i -> {
                        ObjectNode ruleJson = get(rulesJson, i);
                        KubevirtLoadBalancerRule rule = context.codec(
                                KubevirtLoadBalancerRule.class).decode(ruleJson, context);
                        rules.add(rule);
                    });
            builder.rules(rules);
        }

        return builder.build();
    }
}
