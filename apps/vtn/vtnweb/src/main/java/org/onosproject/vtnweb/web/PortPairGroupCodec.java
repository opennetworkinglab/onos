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

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.CoreService;
import org.onosproject.vtnrsc.DefaultPortPairGroup;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.TenantId;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

/**
 * Port Pair Group JSON codec.
 */
public final class PortPairGroupCodec extends JsonCodec<PortPairGroup> {

    private static final String ID = "id";
    private static final String TENANT_ID = "tenant_id";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String PORT_PAIRS = "port_pairs";
    private static final String MISSING_MEMBER_MESSAGE =
            " member is required in PortPairGroup";

    @Override
    public PortPairGroup decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        PortPairGroup.Builder resultBuilder = new DefaultPortPairGroup.Builder();

        CoreService coreService = context.getService(CoreService.class);

        String id = nullIsIllegal(json.get(ID),
                                  ID + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setId(PortPairGroupId.of(id));

        String tenantId = nullIsIllegal(json.get(TENANT_ID),
                                        TENANT_ID + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setTenantId(TenantId.tenantId(tenantId));

        String name = nullIsIllegal(json.get(NAME),
                                    NAME + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setName(name);

        String description = nullIsIllegal(json.get(DESCRIPTION),
                                           DESCRIPTION + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setDescription(description);

        List<PortPairId> list = Lists.newArrayList();
        ArrayNode arrayNode = (ArrayNode) json.path(PORT_PAIRS);
        arrayNode.forEach(i -> list.add(PortPairId.of(i.asText())));
        resultBuilder.setPortPairs(list);

        return resultBuilder.build();
    }

    @Override
    public ObjectNode encode(PortPairGroup portPairGroup, CodecContext context) {
        checkNotNull(portPairGroup, "port pair group cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(ID, portPairGroup.portPairGroupId().toString())
                .put(TENANT_ID, portPairGroup.tenantId().toString())
                .put(NAME, portPairGroup.name())
                .put(DESCRIPTION, portPairGroup.description())
                .put(PORT_PAIRS, portPairGroup.portPairs().toString());
        return result;
    }
}
