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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.packet.DefaultPacketRequest;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketRequest;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Codec for the PacketRequest class.
 */

// TODO: Needs unit test

public class PacketRequestCodec extends JsonCodec<PacketRequest> {

    // JSON field names
    static final String TRAFFIC_SELECTOR = "selector";
    static final String PRIORITY = "priority";
    static final String APP_ID = "appId";
    static final String NODE_ID = "nodeId";
    static final String DEVICE_ID = "deviceId";

    private static final String NULL_OBJECT_MSG = "PacketRequest cannot be null";
    private static final String MISSING_MEMBER_MSG = " member is required in PacketRequest";
    public static final String REST_APP_ID = "org.onosproject.rest";

    @Override
    public ObjectNode encode(PacketRequest packetRequest, CodecContext context) {
        checkNotNull(packetRequest, NULL_OBJECT_MSG);

        final JsonCodec<TrafficSelector> trafficSelectorCodec =
               context.codec(TrafficSelector.class);
        final ObjectNode result = context.mapper().createObjectNode()
                .put(NODE_ID, packetRequest.nodeId().toString())
                .put(PRIORITY, packetRequest.priority().name())
                .put(APP_ID, packetRequest.appId().toString());
        if (packetRequest.deviceId().isPresent()) {
            result.put(DEVICE_ID, packetRequest.deviceId().get().toString());
        }

        result.set(TRAFFIC_SELECTOR, trafficSelectorCodec.encode(packetRequest.selector(), context));

        return result;
    }

    @Override
    public PacketRequest decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        final JsonCodec<TrafficSelector> trafficSelectorCodec =
               context.codec(TrafficSelector.class);
        TrafficSelector trafficSelector = trafficSelectorCodec.decode(
                get(json, TRAFFIC_SELECTOR), context);
        NodeId nodeId = NodeId.nodeId(extractMember(NODE_ID, json));
        PacketPriority priority = PacketPriority.valueOf(extractMember(PRIORITY, json));

        CoreService coreService = context.getService(CoreService.class);
        // TODO check appId (currently hardcoded - should it be read from json node?)
        ApplicationId appId = coreService.registerApplication(REST_APP_ID);

        DeviceId deviceId = null;
        JsonNode node = json.get(DEVICE_ID);
        if (node != null) {
             deviceId = DeviceId.deviceId(node.asText());
        }

        return new DefaultPacketRequest(trafficSelector, priority, appId, nodeId, Optional.ofNullable(deviceId));
    }

    /**
     * Extract member from JSON ObjectNode.
     *
     * @param key key for which value is needed
     * @param json JSON ObjectNode
     * @return member value
     */
    private String extractMember(String key, ObjectNode json) {
        return nullIsIllegal(json.get(key), key + MISSING_MEMBER_MSG).asText();
    }
}
