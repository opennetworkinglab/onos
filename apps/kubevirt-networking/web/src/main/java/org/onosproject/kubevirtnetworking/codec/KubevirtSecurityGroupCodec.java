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
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtSecurityGroup;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroup;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupRule;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubevirt security group codec used for serializing and de-serializing JSON string.
 */
public final class KubevirtSecurityGroupCodec extends JsonCodec<KubevirtSecurityGroup> {

    private final Logger log = getLogger(getClass());

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String RULES = "rules";

    private static final String MISSING_MESSAGE = " is required in KubevirtSecurityGroup";

    @Override
    public ObjectNode encode(KubevirtSecurityGroup sg, CodecContext context) {
        checkNotNull(sg, "Kubevirt Security Group cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(ID, sg.id())
                .put(NAME, sg.name());

        if (sg.description() != null) {
            result.put(DESCRIPTION, sg.description());
        }

        if (sg.rules() != null && !sg.rules().isEmpty()) {
            ArrayNode rules = context.mapper().createArrayNode();
            sg.rules().forEach(rule -> {
                ObjectNode ruleJson = context.codec(
                        KubevirtSecurityGroupRule.class).encode(rule, context);
                rules.add(ruleJson);
            });
            result.set(RULES, rules);
        }

        return result;
    }

    @Override
    public KubevirtSecurityGroup decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String id = nullIsIllegal(json.get(ID).asText(), ID + MISSING_MESSAGE);
        String name = nullIsIllegal(json.get(NAME).asText(), NAME + MISSING_MESSAGE);

        KubevirtSecurityGroup.Builder builder = DefaultKubevirtSecurityGroup.builder()
                .id(id)
                .name(name);

        JsonNode description = json.get(DESCRIPTION);
        if (description != null) {
            builder.description(description.asText());
        }

        JsonNode rulesJson = json.get(RULES);
        if (rulesJson != null) {
            Set<KubevirtSecurityGroupRule> rules = new HashSet<>();
            IntStream.range(0, rulesJson.size())
                    .forEach(i -> {
                        ObjectNode ruleJson = get(rulesJson, i);
                        KubevirtSecurityGroupRule rule = context.codec(
                                KubevirtSecurityGroupRule.class).decode(ruleJson, context);
                        rules.add(rule);
                    });
            builder.rules(rules);
        }

        return builder.build();
    }
}
