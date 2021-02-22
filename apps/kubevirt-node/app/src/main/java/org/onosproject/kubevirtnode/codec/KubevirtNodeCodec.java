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
package org.onosproject.kubevirtnode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.kubevirtnode.api.DefaultKubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeState;
import org.onosproject.kubevirtnode.api.KubevirtPhyInterface;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.onosproject.kubevirtnode.api.Constants.DATA_IP;
import static org.onosproject.kubevirtnode.api.Constants.HOST_NAME;
import static org.onosproject.kubevirtnode.api.Constants.MANAGEMENT_IP;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubevirt node codec used for serializing and de-serializing JSON string.
 */
public final class KubevirtNodeCodec extends JsonCodec<KubevirtNode> {

    private final Logger log = getLogger(getClass());

    private static final String TYPE = "type";
    private static final String INTEGRATION_BRIDGE = "integrationBridge";
    private static final String TUNNEL_BRIDGE = "tunnelBridge";
    private static final String STATE = "state";
    private static final String PHYSICAL_INTERFACES = "phyIntfs";
    private static final String GATEWAY_BRIDGE_NAME = "gatewayBridgeName";

    private static final String MISSING_MESSAGE = " is required in OpenstackNode";

    @Override
    public ObjectNode encode(KubevirtNode node, CodecContext context) {
        checkNotNull(node, "Kubevirt node cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(HOST_NAME, node.hostname())
                .put(TYPE, node.type().name())
                .put(STATE, node.state().name())
                .put(MANAGEMENT_IP, node.managementIp().toString());

        // serialize integration bridge config
        if (node.intgBridge() != null) {
            result.put(INTEGRATION_BRIDGE, node.intgBridge().toString());
        }

        // serialize tunnel bridge config
        if (node.tunBridge() != null) {
            result.put(TUNNEL_BRIDGE, node.tunBridge().toString());
        }

        // serialize data IP only if it presents
        if (node.dataIp() != null) {
            result.put(DATA_IP, node.dataIp().toString());
        }

        // serialize physical interfaces, it is valid only if any of physical interface presents
        if (node.phyIntfs() != null && !node.phyIntfs().isEmpty()) {
            ArrayNode phyIntfs = context.mapper().createArrayNode();
            node.phyIntfs().forEach(phyIntf -> {
                ObjectNode phyIntfJson =
                        context.codec(KubevirtPhyInterface.class).encode(phyIntf, context);
                phyIntfs.add(phyIntfJson);
            });
            result.set(PHYSICAL_INTERFACES, phyIntfs);
        }

        // serialize external bridge if exist
        if (node.gatewayBridgeName() != null) {
            result.put(GATEWAY_BRIDGE_NAME, node.gatewayBridgeName());
        }

        return result;
    }

    @Override
    public KubevirtNode decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String hostname = nullIsIllegal(json.get(HOST_NAME).asText(),
                HOST_NAME + MISSING_MESSAGE);
        String type = nullIsIllegal(json.get(TYPE).asText(),
                TYPE + MISSING_MESSAGE);
        String mIp = nullIsIllegal(json.get(MANAGEMENT_IP).asText(),
                MANAGEMENT_IP + MISSING_MESSAGE);

        KubevirtNode.Builder nodeBuilder = DefaultKubevirtNode.builder()
                .hostname(hostname)
                .type(KubevirtNode.Type.valueOf(type))
                .state(KubevirtNodeState.INIT)
                .managementIp(IpAddress.valueOf(mIp));

        if (json.get(DATA_IP) != null) {
            nodeBuilder.dataIp(IpAddress.valueOf(json.get(DATA_IP).asText()));
        }

        JsonNode intBridgeJson = json.get(INTEGRATION_BRIDGE);
        if (intBridgeJson != null) {
            nodeBuilder.intgBridge(DeviceId.deviceId(intBridgeJson.asText()));
        }

        JsonNode tunBridgeJson = json.get(TUNNEL_BRIDGE);
        if (tunBridgeJson != null) {
            nodeBuilder.tunBridge(DeviceId.deviceId(tunBridgeJson.asText()));
        }

        // parse physical interfaces
        List<KubevirtPhyInterface> phyIntfs = new ArrayList<>();
        JsonNode phyIntfsJson = json.get(PHYSICAL_INTERFACES);
        if (phyIntfsJson != null) {
            final JsonCodec<KubevirtPhyInterface>
                    phyIntfCodec = context.codec(KubevirtPhyInterface.class);

            IntStream.range(0, phyIntfsJson.size()).forEach(i -> {
                ObjectNode intfJson = get(phyIntfsJson, i);
                phyIntfs.add(phyIntfCodec.decode(intfJson, context));
            });
        }
        nodeBuilder.phyIntfs(phyIntfs);

        JsonNode externalBridgeJson = json.get(GATEWAY_BRIDGE_NAME);
        if (externalBridgeJson != null) {
            nodeBuilder.gatewayBridgeName(externalBridgeJson.asText());
        }

        log.trace("node is {}", nodeBuilder.build().toString());

        return nodeBuilder.build();
    }
}
