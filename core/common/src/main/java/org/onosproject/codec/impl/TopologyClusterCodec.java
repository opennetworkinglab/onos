/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.topology.TopologyCluster;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Topology cluster JSON codec.
 */
public final class TopologyClusterCodec extends JsonCodec<TopologyCluster> {

    @Override
    public ObjectNode encode(TopologyCluster cluster, CodecContext context) {
        checkNotNull(cluster, "Cluster cannot be null");

        return context.mapper().createObjectNode()
                .put("id", cluster.id().index())
                .put("deviceCount", cluster.deviceCount())
                .put("linkCount", cluster.linkCount())
                .put("root", cluster.root().toString());
    }
}
