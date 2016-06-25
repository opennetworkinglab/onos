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
import org.onosproject.vtnrsc.FloatingIp;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * FloatingIp JSON codec.
 */
public final class FloatingIpCodec extends JsonCodec<FloatingIp> {
    @Override
    public ObjectNode encode(FloatingIp floatingIp, CodecContext context) {
        checkNotNull(floatingIp, "floatingIp cannot be null");
        ObjectNode result = context
                .mapper()
                .createObjectNode()
                .put("id", floatingIp.id().floatingIpId().toString())
                .put("floating_network_id", floatingIp.networkId().toString())
                .put("router_id",
                     floatingIp.routerId() == null ? null : floatingIp
                             .routerId().routerId())
                .put("tenant_id", floatingIp.tenantId().toString())
                .put("port_id",
                     floatingIp.portId() == null ? null : floatingIp.portId()
                             .toString())
                .put("fixed_ip_address",
                     floatingIp.fixedIp() == null ? null : floatingIp.fixedIp()
                             .toString())
                .put("floating_ip_address", floatingIp.floatingIp().toString())
                .put("status", floatingIp.status().toString());
        return result;
    }

    public ObjectNode extracFields(FloatingIp floatingIp, CodecContext context,
                                   List<String> fields) {
        checkNotNull(floatingIp, "floatingIp cannot be null");
        ObjectNode result = context.mapper().createObjectNode();
        Iterator<String> i = fields.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.equals("floating_network_id")) {
                result.put("floating_network_id", floatingIp.networkId()
                        .toString());
            }
            if (s.equals("router_id")) {
                result.put("router_id",
                           floatingIp.routerId() == null ? null : floatingIp
                                   .routerId().routerId());
            }
            if (s.equals("tenant_id")) {
                result.put("tenant_id", floatingIp.tenantId().toString());
            }
            if (s.equals("port_id")) {
                result.put("port_id",
                           floatingIp.portId() == null ? null : floatingIp
                                   .portId().toString());
            }
            if (s.equals("id")) {
                result.put("id", floatingIp.id().floatingIpId().toString());
            }
            if (s.equals("fixed_ip_address")) {
                result.put("fixed_ip_address",
                           floatingIp.fixedIp() == null ? null : floatingIp
                                   .fixedIp().toString());
            }
            if (s.equals("floating_ip_address")) {
                result.put("floating_ip_address", floatingIp.floatingIp()
                        .toString());
            }
            if (s.equals("status")) {
                result.put("status", floatingIp.status().toString());
            }
        }
        return result;
    }
}
