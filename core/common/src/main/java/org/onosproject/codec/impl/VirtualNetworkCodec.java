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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.virtual.DefaultVirtualNetwork;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualNetwork;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Codec for the VirtualNetwork class.
 */
public class VirtualNetworkCodec extends JsonCodec<VirtualNetwork> {

    // JSON field names
    private static final String NETWORK_ID = "networkId";
    private static final String TENANT_ID = "tenantId";

    private static final String NULL_OBJECT_MSG = "VirtualNetwork cannot be null";
    private static final String MISSING_MEMBER_MSG = " member is required in VirtualNetwork";

    @Override
    public ObjectNode encode(VirtualNetwork vnet, CodecContext context) {
        checkNotNull(vnet, NULL_OBJECT_MSG);

        ObjectNode result = context.mapper().createObjectNode()
                .put(NETWORK_ID, vnet.id().toString())
                .put(TENANT_ID, vnet.tenantId().toString());

        return result;
    }

    @Override
    public VirtualNetwork decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        NetworkId nId = NetworkId.networkId(Long.parseLong(extractMember(NETWORK_ID, json)));
        TenantId tId = TenantId.tenantId(extractMember(TENANT_ID, json));
        return new DefaultVirtualNetwork(nId, tId);
    }

    private String extractMember(String key, ObjectNode json) {
        return nullIsIllegal(json.get(key), key + MISSING_MEMBER_MSG).asText();
    }
}
