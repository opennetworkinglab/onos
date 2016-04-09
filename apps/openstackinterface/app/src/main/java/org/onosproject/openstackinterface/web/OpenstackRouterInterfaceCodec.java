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
package org.onosproject.openstackinterface.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstackinterface.OpenstackRouterInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.google.common.base.Preconditions.checkNotNull;
/**
 * Implementation of the OpenstackRouterInterface Codec.
 */
public class OpenstackRouterInterfaceCodec extends JsonCodec<OpenstackRouterInterface> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ID = "id";
    private static final String TENANT_ID = "tenant_id";
    private static final String SUBNET_ID = "subnet_id";
    private static final String PORT_ID = "port_id";

    /**
     * Decodes the OpenstackRouterInterface.
     *
     * @param json    JSON to decode
     * @param context decoding context
     * @return OpenstackRouterInterface
     */
    @Override
    public OpenstackRouterInterface decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }
        JsonNode routerIfInfo = json;

        String id = checkNotNull(routerIfInfo.path(ID).asText());
        String tenantId = checkNotNull(routerIfInfo.path(TENANT_ID).asText());
        String subnetId = checkNotNull(routerIfInfo.path(SUBNET_ID).asText());
        String portId = checkNotNull(routerIfInfo.path(PORT_ID).asText());

        OpenstackRouterInterface.Builder osBuilder = new OpenstackRouterInterface.Builder()
                .id(id)
                .tenantId(tenantId)
                .subnetId(subnetId)
                .portId(portId);

        return osBuilder.build();
    }
}
