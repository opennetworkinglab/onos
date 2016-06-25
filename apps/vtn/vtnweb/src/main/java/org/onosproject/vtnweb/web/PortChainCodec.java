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
package org.onosproject.vtnweb.web;


import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

import java.util.List;
import java.util.UUID;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.DefaultPortChain;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.PortPairGroupId;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

/**
 * Port chain JSON codec.
 */
public final class PortChainCodec extends JsonCodec<PortChain> {

    private static final String ID = "id";
    private static final String TENANT_ID = "tenant_id";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String PORT_PAIR_GROUPS = "port_pair_groups";
    private static final String FLOW_CLASSIFIERS = "flow_classifiers";
    private static final String MISSING_MEMBER_MESSAGE =
            " member is required in PortChain";

    @Override
    public PortChain decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        PortChain.Builder resultBuilder = new DefaultPortChain.Builder();

        String id = nullIsIllegal(json.get(ID),
                                  ID + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setId(PortChainId.of(id));

        String tenantId = nullIsIllegal(json.get(TENANT_ID),
                                        TENANT_ID + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setTenantId(TenantId.tenantId(tenantId));

        String name = nullIsIllegal(json.get(NAME),
                                    NAME + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setName(name);

        String description = nullIsIllegal(json.get(DESCRIPTION),
                                           DESCRIPTION + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setDescription(description);

        ArrayNode arrayNode = (ArrayNode) json.path(PORT_PAIR_GROUPS);
        if (arrayNode != null) {
            List<PortPairGroupId> list = Lists.newArrayList();
            arrayNode.forEach(i -> list.add(PortPairGroupId.of(i.asText())));
            resultBuilder.setPortPairGroups(list);
        }

        arrayNode = (ArrayNode) json.path(FLOW_CLASSIFIERS);
        if (arrayNode != null) {
            List<FlowClassifierId> list = Lists.newArrayList();
            arrayNode.forEach(i -> list.add(FlowClassifierId.of(UUID.fromString(i.asText()))));
            resultBuilder.setFlowClassifiers(list);
        }

        return resultBuilder.build();
    }

    @Override
    public ObjectNode encode(PortChain portChain, CodecContext context) {
        checkNotNull(portChain, "port pair cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(ID, portChain.portChainId().toString())
                .put(TENANT_ID, portChain.tenantId().toString())
                .put(NAME, portChain.name())
                .put(DESCRIPTION, portChain.description())
                .put(PORT_PAIR_GROUPS, portChain.portPairGroups().toString())
                .put(FLOW_CLASSIFIERS, portChain.flowClassifiers().toString());
        return result;
    }
}
