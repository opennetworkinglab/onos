/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.protobuf.models.cluster;

import org.onosproject.cluster.NodeId;
import org.onosproject.grpc.net.cluster.models.NodeIdProtoOuterClass;

/**
 * gRPC NodeIdProto message to equivalent ONOS NodeId conversion related utilities.
 */
public final class NodeIdProtoTranslator {

    /**
     * Translates gRPC NodeId to {@link NodeId}.
     *
     * @param nodeId gRPC message
     * @return {@link NodeId}
     */
    public static NodeId translate(NodeIdProtoOuterClass.NodeIdProto nodeId) {
        if (nodeId.equals(NodeIdProtoOuterClass.NodeIdProto.getDefaultInstance())) {
            return null;
        }

        return NodeId.nodeId(nodeId.getNodeId());
    }

    /**
     * Translates {@link NodeId} to gRPC NodeId message.
     *
     * @param nodeId {@link NodeId}
     * @return gRPC NodeId message
     */
    public static NodeIdProtoOuterClass.NodeIdProto translate(NodeId nodeId) {

        if (nodeId != null) {
            return NodeIdProtoOuterClass.NodeIdProto.newBuilder()
                    .setNodeId(nodeId.id())
                    .build();
        }

        return NodeIdProtoOuterClass.NodeIdProto.getDefaultInstance();
    }

    // Utility class not intended for instantiation.
    private NodeIdProtoTranslator() {}
}
