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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.cluster.DefaultControllerNode.DEFAULT_PORT;

/**
 * Device JSON codec.
 */
public final class ControllerNodeCodec extends JsonCodec<ControllerNode> {

    @Override
    public ObjectNode encode(ControllerNode node, CodecContext context) {
        checkNotNull(node, "Controller node cannot be null");
        ClusterService service = context.getService(ClusterService.class);
        return context.mapper().createObjectNode()
                .put("id", node.id().toString())
                .put("ip", node.ip().toString())
                .put("tcpPort", node.tcpPort())
                .put("status", service.getState(node.id()).toString());
    }


    @Override
    public ControllerNode decode(ObjectNode json, CodecContext context) {
        checkNotNull(json, "JSON cannot be null");
        String ip = json.path("ip").asText();
        return new DefaultControllerNode(new NodeId(json.path("id").asText(ip)),
                                         IpAddress.valueOf(ip),
                                         json.path("tcpPort").asInt(DEFAULT_PORT));
    }


}
