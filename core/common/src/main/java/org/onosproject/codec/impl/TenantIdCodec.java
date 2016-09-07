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
import org.onosproject.incubator.net.virtual.TenantId;


import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Codec for the TenantId class.
 */
public class TenantIdCodec extends JsonCodec<TenantId> {

    // JSON field names
    private static final String TENANT_ID = "id";

    private static final String NULL_TENANT_MSG = "TenantId cannot be null";
    private static final String MISSING_MEMBER_MSG = " member is required in TenantId";

    @Override
    public ObjectNode encode(TenantId tenantId, CodecContext context) {
        checkNotNull(tenantId, NULL_TENANT_MSG);

        ObjectNode result = context.mapper().createObjectNode()
                .put(TENANT_ID, tenantId.id());

        return result;
    }

    @Override
    public TenantId decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        TenantId tenantId = TenantId.tenantId(extractMember(TENANT_ID, json));

        return tenantId;
    }

    private String extractMember(String key, ObjectNode json) {
        return nullIsIllegal(json.get(key), key + MISSING_MEMBER_MSG).asText();
    }
}
