/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.app.vtnrsc.web;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.app.vtnrsc.Subnet;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Subnet JSON codec.
 */
public final class SubnetCodec extends JsonCodec<Subnet> {
    @Override
    public ObjectNode encode(Subnet subnet, CodecContext context) {
        checkNotNull(subnet, "Subnet cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("id", subnet.id().toString())
                .put("gate_ip", subnet.gatewayIp().toString())
                .put("network_id", subnet.networkId().toString())
                .put("name", subnet.subnetName().toString())
                .put("ip_version", subnet.ipVersion().toString())
                .put("cidr", subnet.cidr().toString())
                .put("shared", subnet.shared())
                .put("enabled_dchp", subnet.dhcpEnabled())
                .put("tenant_id", subnet.tenantId().toString());
        result.set("alloction_pools", new AllocationPoolsCodec().encode(subnet
                .allocationPools(), context));
        result.set("host_routes",
                   new HostRoutesCodec().encode(subnet.hostRoutes(), context));
        return result;
    }
}
