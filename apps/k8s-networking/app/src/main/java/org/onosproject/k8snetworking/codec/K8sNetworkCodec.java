/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.k8snetworking.api.DefaultK8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubernetes network codec used for serializing and de-serializing JSON string.
 */
public final class K8sNetworkCodec extends JsonCodec<K8sNetwork> {

    private final Logger log = getLogger(getClass());

    private static final String NETWORK_ID = "networkId";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String MTU = "mtu";
    private static final String SEGMENT_ID = "segmentId";
    private static final String CIDR = "cidr";

    private static final String MISSING_MESSAGE = " is required in K8sNetwork";

    @Override
    public ObjectNode encode(K8sNetwork network, CodecContext context) {
        checkNotNull(network, "Kubernetes network cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(NETWORK_ID, network.networkId())
                .put(NAME, network.name())
                .put(CIDR, network.cidr());

        if (network.type() != null) {
            result.put(TYPE, network.type().name());
        }

        if (network.segmentId() != null) {
            result.put(SEGMENT_ID, network.segmentId());
        }

        if (network.mtu() != null) {
            result.put(MTU, network.mtu());
        }

        return result;
    }

    @Override
    public K8sNetwork decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String networkId = nullIsIllegal(json.get(NETWORK_ID).asText(),
                NETWORK_ID + MISSING_MESSAGE);
        String name = nullIsIllegal(json.get(NAME).asText(),
                NAME + MISSING_MESSAGE);
        String cidr = nullIsIllegal(json.get(CIDR).asText(),
                CIDR + MISSING_MESSAGE);

        JsonNode type = json.get(TYPE);
        JsonNode segmentId = json.get(SEGMENT_ID);

        DefaultK8sNetwork.Builder networkBuilder = DefaultK8sNetwork.builder()
                .networkId(networkId)
                .name(name)
                .cidr(cidr);

        if (type != null) {
            networkBuilder.type(K8sNetwork.Type.valueOf(type.asText()));
        }

        if (segmentId != null) {
            networkBuilder.segmentId(segmentId.asText());
        }

        if (json.get(MTU) != null) {
            networkBuilder.mtu(json.get(MTU).asInt());
        }

        return networkBuilder.build();
    }
}
