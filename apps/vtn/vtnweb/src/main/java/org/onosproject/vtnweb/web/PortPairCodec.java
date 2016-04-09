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

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.CoreService;
import org.onosproject.vtnrsc.DefaultPortPair;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.TenantId;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Port Pair JSON codec.
 */
public final class PortPairCodec extends JsonCodec<PortPair> {

    private static final String ID = "id";
    private static final String TENANT_ID = "tenant_id";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String INGRESS = "ingress";
    private static final String EGRESS = "egress";
    private static final String MISSING_MEMBER_MESSAGE =
            " member is required in PortPair";

    @Override
    public PortPair decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        PortPair.Builder resultBuilder = new DefaultPortPair.Builder();

        CoreService coreService = context.getService(CoreService.class);

        String id = nullIsIllegal(json.get(ID),
                                  ID + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setId(PortPairId.of(id));

        String tenantId = nullIsIllegal(json.get(TENANT_ID),
                                        TENANT_ID + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setTenantId(TenantId.tenantId(tenantId));

        String name = nullIsIllegal(json.get(NAME),
                                    NAME + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setName(name);

        String description = nullIsIllegal(json.get(DESCRIPTION),
                                           DESCRIPTION + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setDescription(description);

        String ingressPort = nullIsIllegal(json.get(INGRESS),
                                           INGRESS + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setIngress(ingressPort);

        String egressPort = nullIsIllegal(json.get(EGRESS),
                                          EGRESS + MISSING_MEMBER_MESSAGE).asText();
        resultBuilder.setEgress(egressPort);

        return resultBuilder.build();
    }

    @Override
    public ObjectNode encode(PortPair portPair, CodecContext context) {
        checkNotNull(portPair, "port pair cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(ID, portPair.portPairId().toString())
                .put(TENANT_ID, portPair.tenantId().toString())
                .put(NAME, portPair.name())
                .put(DESCRIPTION, portPair.description())
                .put(INGRESS, portPair.ingress())
                .put(EGRESS, portPair.egress());
        return result;
    }
}
