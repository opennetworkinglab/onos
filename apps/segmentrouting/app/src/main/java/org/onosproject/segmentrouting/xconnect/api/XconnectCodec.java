/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.segmentrouting.xconnect.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Codec for Xconnect.
 */
public class XconnectCodec extends JsonCodec<XconnectDesc> {
    static final String DEVICE_ID = "deviceId";
    static final String VLAN_ID = "vlanId";
    static final String ENDPOINTS = "endpoints";

    private static Logger log = LoggerFactory.getLogger(XconnectCodec.class);

    @Override
    public ObjectNode encode(XconnectDesc desc, CodecContext context) {
        final ObjectNode result = context.mapper().createObjectNode();
        result.put(DEVICE_ID, desc.key().deviceId().toString());
        result.put(VLAN_ID, desc.key().vlanId().toShort());
        final ArrayNode portNode = result.putArray(ENDPOINTS);
        desc.endpoints().forEach(endpoint -> portNode.add(endpoint.toString()));

        return result;
    }

    @Override
    public XconnectDesc decode(ObjectNode json, CodecContext context) {
        DeviceId deviceId = DeviceId.deviceId(json.path(DEVICE_ID).asText());
        VlanId vlanId = VlanId.vlanId(json.path(VLAN_ID).asText());

        Set<XconnectEndpoint> endpoints = Sets.newHashSet();
        JsonNode endpointNodes = json.get(ENDPOINTS);
        if (endpointNodes != null) {
            endpointNodes.forEach(endpointNode -> endpoints.add(XconnectEndpoint.fromString(endpointNode.asText())));
        }

        XconnectKey key = new XconnectKey(deviceId, vlanId);
        return new XconnectDesc(key, endpoints);
    }
}
