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
import org.onosproject.incubator.net.virtual.DefaultVirtualLink;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.net.Link;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Codec for the VirtualLink class.
 */
public class VirtualLinkCodec extends JsonCodec<VirtualLink> {

    // JSON field names
    private static final String NETWORK_ID = "networkId";

    private static final String NULL_OBJECT_MSG = "VirtualLink cannot be null";
    private static final String MISSING_MEMBER_MSG = " member is required in VirtualLink";

    @Override
    public ObjectNode encode(VirtualLink vLink, CodecContext context) {
        checkNotNull(vLink, NULL_OBJECT_MSG);

        ObjectNode result = context.mapper().createObjectNode()
                .put(NETWORK_ID, vLink.networkId().toString());
        JsonCodec<Link> codec = context.codec(Link.class);
        ObjectNode linkResult = codec.encode(vLink, context);
        result.setAll(linkResult);
        return result;
    }

    @Override
    public VirtualLink decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }
        JsonCodec<Link> codec = context.codec(Link.class);
        Link link = codec.decode(json, context);
        NetworkId nId = NetworkId.networkId(Long.parseLong(extractMember(NETWORK_ID, json)));
        return DefaultVirtualLink.builder()
                .networkId(nId)
                .src(link.src())
                .dst(link.dst())
                .build();
    }

    /**
     * Extract member from JSON ObjectNode.
     *
     * @param key  key for which value is needed
     * @param json JSON ObjectNode
     * @return member value
     */
    private String extractMember(String key, ObjectNode json) {
        return nullIsIllegal(json.get(key), key + MISSING_MEMBER_MSG).asText();
    }
}
