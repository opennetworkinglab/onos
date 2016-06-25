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

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.vtnrsc.RouterGateway;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Subnet Router Gateway Info codec.
 */
public class RouterGatewayInfoCodec extends JsonCodec<RouterGateway> {
    @Override
    public ObjectNode encode(RouterGateway routerGateway, CodecContext context) {
        checkNotNull(routerGateway, "routerGateway cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("network_id", routerGateway.networkId().toString());
        result.set("external_fixed_ips", new FixedIpCodec()
                .encode(routerGateway.externalFixedIps(), context));
        return result;
    }
}
