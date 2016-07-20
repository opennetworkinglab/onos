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
package org.onosproject.segmentrouting.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.segmentrouting.DefaultTunnel;
import org.onosproject.segmentrouting.Tunnel;

import java.util.ArrayList;
import java.util.List;

/**
 * Codec of Tunnel class.
 */
public final class TunnelCodec extends JsonCodec<Tunnel> {

    // JSON field names
    private static final String TUNNEL_ID = "tunnel_id";
    private static final String GROUP_ID = "group_id";
    private static final String LABEL_PATH = "label_path";

    @Override
    public ObjectNode encode(Tunnel tunnel, CodecContext context) {
        final ObjectNode result = context.mapper().createObjectNode()
                .put(TUNNEL_ID, tunnel.id());

        result.put(GROUP_ID, tunnel.groupId());

        final ArrayNode jsonLabelIds = result.putArray(LABEL_PATH);

        tunnel.labelIds().forEach(label -> jsonLabelIds.add(label.intValue()));

        return result;
    }

    @Override
    public DefaultTunnel decode(ObjectNode json, CodecContext context) {

        String tid = json.path(TUNNEL_ID).asText();
        List<Integer> labels = new ArrayList<>();

        if (!json.path(LABEL_PATH).isMissingNode()) {
            ArrayNode labelArray = (ArrayNode) json.path(LABEL_PATH);
            for (JsonNode o : labelArray) {
                labels.add(o.asInt());
            }
        }

        return new DefaultTunnel(tid, labels);
    }

}
