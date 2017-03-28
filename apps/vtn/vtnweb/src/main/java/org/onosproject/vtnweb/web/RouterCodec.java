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

import java.util.Iterator;
import java.util.List;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.vtnrsc.Router;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Router JSON codec.
 */
public class RouterCodec extends JsonCodec<Router> {
    @Override
    public ObjectNode encode(Router router, CodecContext context) {
        checkNotNull(router, "router cannot be null");
        ObjectNode result = context
                .mapper()
                .createObjectNode()
                .put("id", router.id().routerId())
                .put("status", router.status().toString())
                .put("name", router.name().toString())
                .put("admin_state_up", router.adminStateUp())
                .put("tenant_id", router.tenantId().toString())
                .put("routes",
                     router.routes() == null ? null : router.routes()
                             .toString());
        result.set("external_gateway_info",
                   router.externalGatewayInfo() == null ? null
                                                       : new RouterGatewayInfoCodec()
                                                        .encode(router.externalGatewayInfo(), context));

        return result;
    }

    public ObjectNode extracFields(Router router, CodecContext context,
                                   List<String> fields) {
        checkNotNull(router, "router cannot be null");
        ObjectNode result = context.mapper().createObjectNode();
        Iterator<String> i = fields.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if ("id".equals(s)) {
                result.put("id", router.id().routerId());
            }
            if ("status".equals(s)) {
                result.put("status", router.status().toString());
            }
            if ("name".equals(s)) {
                result.put("name", router.name().toString());
            }
            if ("admin_state_up".equals(s)) {
                result.put("admin_state_up", router.adminStateUp());
            }
            if ("tenant_id".equals(s)) {
                result.put("tenant_id", router.tenantId().toString());
            }
            if ("routes".equals(s)) {
                result.put("routes", router.routes() == null ? null : router
                        .routes().toString());
            }
            if ("external_gateway_info".equals(s)) {
                result.set("external_gateway_info",
                           router.externalGatewayInfo() == null ? null
                                                               : new RouterGatewayInfoCodec()
                                                                       .encode(router.externalGatewayInfo(),
                                                                               context));
            }
        }
        return result;
    }
}
