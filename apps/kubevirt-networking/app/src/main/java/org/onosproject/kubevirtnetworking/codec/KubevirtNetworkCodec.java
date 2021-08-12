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
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtHostRoute;
import org.onosproject.kubevirtnetworking.api.KubevirtIpPool;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubevirt network codec used for serializing and de-serializing JSON string.
 */
public final class KubevirtNetworkCodec extends JsonCodec<KubevirtNetwork> {

    private final Logger log = getLogger(getClass());

    private static final String NETWORK_ID = "networkId";
    private static final String TYPE = "type";
    private static final String NAME = "name";
    private static final String MTU = "mtu";
    private static final String SEGMENT_ID = "segmentId";
    private static final String GATEWAY_IP = "gatewayIp";
    private static final String DEFAULT_ROUTE = "defaultRoute";
    private static final String CIDR = "cidr";
    private static final String HOST_ROUTES = "hostRoutes";
    private static final String IP_POOL = "ipPool";
    private static final String DNSES = "dnses";

    private static final String MISSING_MESSAGE = " is required in KubevirtNetwork";

    @Override
    public ObjectNode encode(KubevirtNetwork network, CodecContext context) {
        checkNotNull(network, "Kubevirt network cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(NETWORK_ID, network.networkId())
                .put(TYPE, network.type().name())
                .put(NAME, network.name())
                .put(MTU, network.mtu())
                .put(GATEWAY_IP, network.gatewayIp().toString())
                .put(DEFAULT_ROUTE, network.defaultRoute())
                .put(CIDR, network.cidr());

        if (network.segmentId() != null) {
            result.put(SEGMENT_ID, network.segmentId());
        }

        if (network.hostRoutes() != null && !network.hostRoutes().isEmpty()) {
            ArrayNode hostRoutes = context.mapper().createArrayNode();
            network.hostRoutes().forEach(hostRoute -> {
                ObjectNode hostRouteJson =
                        context.codec(KubevirtHostRoute.class).encode(hostRoute, context);
                hostRoutes.add(hostRouteJson);
            });
            result.set(HOST_ROUTES, hostRoutes);
        }

        if (network.ipPool() != null) {
            ObjectNode ipPoolJson = context.codec(KubevirtIpPool.class).encode(network.ipPool(), context);
            result.set(IP_POOL, ipPoolJson);
        }

        if (network.dnses() != null && !network.dnses().isEmpty()) {
            ArrayNode dnses = context.mapper().createArrayNode();
            network.dnses().forEach(dns -> {
                dnses.add(dns.toString());
            });
            result.set(DNSES, dnses);
        }

        return result;
    }

    @Override
    public KubevirtNetwork decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String networkId = nullIsIllegal(json.get(NETWORK_ID).asText(),
                NETWORK_ID + MISSING_MESSAGE);
        String type = nullIsIllegal(json.get(TYPE).asText(),
                TYPE + MISSING_MESSAGE);
        String name = nullIsIllegal(json.get(NAME).asText(),
                NAME + MISSING_MESSAGE);
        Integer mtu = nullIsIllegal(json.get(MTU).asInt(),
                MTU + MISSING_MESSAGE);
        String gatewayIp = nullIsIllegal(json.get(GATEWAY_IP).asText(),
                GATEWAY_IP + MISSING_MESSAGE);
        boolean defaultRoute = nullIsIllegal(json.get(DEFAULT_ROUTE).asBoolean(),
                DEFAULT_ROUTE + MISSING_MESSAGE);
        String cidr = nullIsIllegal(json.get(CIDR).asText(),
                CIDR + MISSING_MESSAGE);

        KubevirtNetwork.Builder networkBuilder = DefaultKubevirtNetwork.builder()
                .networkId(networkId)
                .type(KubevirtNetwork.Type.valueOf(type))
                .name(name)
                .mtu(mtu)
                .gatewayIp(IpAddress.valueOf(gatewayIp))
                .defaultRoute(defaultRoute)
                .cidr(cidr);

        if (!type.equals(KubevirtNetwork.Type.FLAT.name())) {
            JsonNode segmentIdJson = json.get(SEGMENT_ID);
            if (segmentIdJson != null) {
                networkBuilder.segmentId(segmentIdJson.asText());
            }
        }

        JsonNode ipPoolJson = json.get(IP_POOL);
        if (ipPoolJson != null) {
            final JsonCodec<KubevirtIpPool>
                    ipPoolCodec = context.codec(KubevirtIpPool.class);
            networkBuilder.ipPool(ipPoolCodec.decode(
                    (ObjectNode) ipPoolJson.deepCopy(), context));
        }

        // parse host routes
        Set<KubevirtHostRoute> hostRoutes = new HashSet<>();
        JsonNode hostRoutesJson = json.get(HOST_ROUTES);
        if (hostRoutesJson != null) {
            final JsonCodec<KubevirtHostRoute>
                    hostRouteCodec = context.codec(KubevirtHostRoute.class);

            IntStream.range(0, hostRoutesJson.size()).forEach(i -> {
                ObjectNode routeJson = get(hostRoutesJson, i);
                hostRoutes.add(hostRouteCodec.decode(routeJson, context));
            });
        }
        networkBuilder.hostRoutes(hostRoutes);

        // parse DNSes
        Set<IpAddress> dnses = new HashSet<>();
        JsonNode dnsesJson = json.get(DNSES);
        if (dnsesJson != null) {
            for (int i = 0; i < dnsesJson.size(); i++) {
                JsonNode dnsJson = dnsesJson.get(i);
                if (dnsJson != null) {
                    dnses.add(IpAddress.valueOf(dnsJson.asText()));
                }
            }
        }
        networkBuilder.dnses(dnses);

        log.trace("Network is {}", networkBuilder.build().toString());

        return networkBuilder.build();
    }
}
