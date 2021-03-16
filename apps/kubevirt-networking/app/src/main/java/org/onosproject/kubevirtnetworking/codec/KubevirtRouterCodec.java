/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtPeerRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubevirt router codec used for serializing and de-serializing JSON string.
 */
public final class KubevirtRouterCodec extends JsonCodec<KubevirtRouter> {

    private final Logger log = getLogger(getClass());

    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String ENABLE_SNAT = "enableSnat";
    private static final String INTERNAL = "internal";
    private static final String EXTERNAL = "external";
    private static final String PEER_ROUTER = "peerRouter";
    private static final String IP_ADDRESS = "ip";
    private static final String MAC_ADDRESS = "mac";
    private static final String NETWORK = "network";
    private static final String GATEWAY = "gateway";

    private static final String MISSING_MESSAGE = " is required in KubevirtRouter";

    @Override
    public ObjectNode encode(KubevirtRouter router, CodecContext context) {
        checkNotNull(router, "Kubevirt router cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(NAME, router.name())
                .put(ENABLE_SNAT, router.enableSnat())
                .put(MAC_ADDRESS, router.mac().toString());

        if (router.description() != null) {
            result.put(DESCRIPTION, router.description());
        }

        if (router.internal() != null && !router.internal().isEmpty()) {
            ArrayNode internal = context.mapper().createArrayNode();
            router.internal().forEach(internal::add);

            result.set(INTERNAL, internal);
        }

        if (router.external() != null && !router.external().isEmpty()) {
            ArrayNode external = context.mapper().createArrayNode();
            router.external().forEach((k, v) -> {
                ObjectNode item = context.mapper().createObjectNode();
                item.put(IP_ADDRESS, k);
                item.put(NETWORK, v);
                external.add(item);
            });
            result.set(EXTERNAL, external);
        }

        if (router.peerRouter() != null) {
            ObjectNode peerRouter = context.mapper().createObjectNode();
            peerRouter.put(IP_ADDRESS, router.peerRouter().ipAddress().toString());

            if (router.peerRouter().macAddress() != null) {
                peerRouter.put(MAC_ADDRESS, router.peerRouter().macAddress().toString());
            }

            result.set(PEER_ROUTER, peerRouter);
        }

        if (router.electedGateway() != null) {
            result.put(GATEWAY, router.electedGateway());
        }

        return result;
    }

    @Override
    public KubevirtRouter decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String name = nullIsIllegal(json.get(NAME).asText(),
                NAME + MISSING_MESSAGE);

        String vrouterMac = nullIsIllegal(json.get(MAC_ADDRESS).asText(),
                MAC_ADDRESS + MISSING_MESSAGE);

        KubevirtRouter.Builder builder = DefaultKubevirtRouter.builder()
                .name(name)
                .mac(MacAddress.valueOf(vrouterMac));

        JsonNode descriptionJson = json.get(DESCRIPTION);
        if (descriptionJson != null) {
            builder.description(descriptionJson.asText());
        }

        JsonNode enableSnatJson = json.get(ENABLE_SNAT);
        if (enableSnatJson != null) {
            builder.enableSnat(enableSnatJson.asBoolean());
        }
        JsonNode electedGwJson = json.get(GATEWAY);
        if (electedGwJson != null) {
            builder.electedGateway(electedGwJson.asText());
        }

        ArrayNode internalJson = (ArrayNode) json.get(INTERNAL);
        Set<String> internal = new HashSet<>();
        if (internalJson != null) {
            for (int i = 0; i < internalJson.size(); i++) {
                internal.add(internalJson.get(i).asText());
            }
            builder.internal(internal);
        }

        ObjectNode externalJson = (ObjectNode) json.get(EXTERNAL);
        if (externalJson != null) {
            Map<String, String> external = ImmutableMap.of(
                    externalJson.get(IP_ADDRESS).asText(),
                    externalJson.get(NETWORK).asText());
            builder.external(external);
        }

        ObjectNode peerRouterJson = (ObjectNode) json.get(PEER_ROUTER);
        if (peerRouterJson != null) {
            JsonNode ipJson = peerRouterJson.get(IP_ADDRESS);
            JsonNode macJson = peerRouterJson.get(MAC_ADDRESS);

            if (ipJson != null && macJson != null) {
                IpAddress ip = IpAddress.valueOf(ipJson.asText());
                MacAddress mac = MacAddress.valueOf(macJson.asText());
                KubevirtPeerRouter peer = new KubevirtPeerRouter(ip, mac);
                builder.peerRouter(peer);
            }

            // if mac address is not specified, we will not add mac address to peer router
            if (ipJson != null && macJson == null) {
                IpAddress ip = IpAddress.valueOf(ipJson.asText());
                KubevirtPeerRouter peer = new KubevirtPeerRouter(ip, null);
                builder.peerRouter(peer);
            }
        }

        return builder.build();
    }
}
