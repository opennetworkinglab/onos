/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.k8snode.codec;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.k8snode.api.DefaultK8sHost;
import org.onosproject.k8snode.api.K8sHost;
import org.onosproject.k8snode.api.K8sHostState;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubernetes host codec used for serializing and de-serializing JSON string.
 */
public final class K8sHostCodec extends JsonCodec<K8sHost> {

    private final Logger log = getLogger(getClass());

    private static final String HOST_IP = "hostIp";
    private static final String NODE_NAMES = "nodeNames";
    private static final String STATE = "state";

    private static final String MISSING_MESSAGE = " is required in K8sHost";

    @Override
    public ObjectNode encode(K8sHost entity, CodecContext context) {
        checkNotNull(entity, "Kubernetes host cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(HOST_IP, entity.hostIp().toString())
                .put(STATE, entity.state().name());

        ArrayNode nodes = context.mapper().createArrayNode();
        entity.nodeNames().forEach(nodes::add);
        result.set(NODE_NAMES, nodes);

        return result;
    }

    @Override
    public K8sHost decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        IpAddress hostIp = IpAddress.valueOf(nullIsIllegal(json.get(HOST_IP).asText(),
                HOST_IP + MISSING_MESSAGE));
        ArrayNode nodeNamesJson = (ArrayNode) json.get(NODE_NAMES);
        Set<String> nodeNames = new HashSet<>();
        nodeNamesJson.forEach(n -> nodeNames.add(n.asText()));

        return DefaultK8sHost.builder()
                .hostIp(hostIp)
                .state(K8sHostState.INIT)
                .nodeNames(nodeNames)
                .build();
    }
}
