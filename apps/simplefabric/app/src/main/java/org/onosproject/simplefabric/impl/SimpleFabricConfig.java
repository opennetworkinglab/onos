/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.simplefabric.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.config.Config;
import org.onosproject.simplefabric.api.FabricNetwork;
import org.onosproject.simplefabric.api.FabricRoute;
import org.onosproject.simplefabric.api.FabricSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Configuration object for prefix config.
 */
public class SimpleFabricConfig extends Config<ApplicationId> {
    public static final String KEY = "simpleFabric";

    private static final String FABRIC_NETWORKS = "fabricNetworks";
    private static final String NAME = "name";
    private static final String INTERFACES = "interfaces";
    private static final String ENCAPSULATION = "encapsulation";
    private static final String IS_FORWARD = "isForward";
    private static final String IS_BROADCAST = "isBroadcast";

    private static final String FABRIC_SUBNETS = "fabricSubnets";
    private static final String PREFIX = "prefix";
    private static final String GATEWAY_IP = "gatewayIp";
    private static final String GATEWAY_MAC = "gatewayMac";
    private static final String NETWORK_NAME = "networkName";

    private static final String FABRIC_ROUTES = "fabricRoutes";
    private static final String NEXT_HOP = "nextHop";

    private static final String NONE_ENCAPSULATION = "NONE";

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Returns all fabric networks in this configuration.
     *
     * @return a set of fabric networks
     */
    public Set<FabricNetwork> fabricNetworks() {
        Set<FabricNetwork> fabricNetworks = Sets.newHashSet();
        JsonNode fabricNetworkNodes = object.get(FABRIC_NETWORKS);
        if (fabricNetworkNodes == null) {
            return fabricNetworks;
        }

        fabricNetworkNodes.forEach(jsonNode -> {
            Set<String> ifaces = Sets.newHashSet();
            JsonNode fabricNetworkIfaces = jsonNode.path(INTERFACES);
            if (fabricNetworkIfaces == null) {
                log.warn("Fabric network interfaces cannot find {}; skip: jsonNode={}", INTERFACES, jsonNode);
            } else if (!fabricNetworkIfaces.toString().isEmpty()) {
                fabricNetworkIfaces.forEach(ifacesNode -> ifaces.add(ifacesNode.asText()));
            }
            String encapsulation = NONE_ENCAPSULATION;   // NONE or VLAN
            if (jsonNode.hasNonNull(ENCAPSULATION)) {
                encapsulation = jsonNode.get(ENCAPSULATION).asText();
            }
            boolean isForward = true;
            if (jsonNode.hasNonNull(IS_FORWARD)) {
                isForward = jsonNode.get(IS_FORWARD).asBoolean();
            }
            boolean isBroadcast = true;
            if (jsonNode.hasNonNull(IS_BROADCAST)) {
                isBroadcast = jsonNode.get(IS_BROADCAST).asBoolean();
            }
            try {
                fabricNetworks.add(DefaultFabricNetwork.builder()
                                    .name(jsonNode.get(NAME).asText())
                                    .interfaceNames(ifaces)
                                    .encapsulation(EncapsulationType.enumFromString(encapsulation))
                                    .forward(isForward)
                                    .broadcast(isBroadcast)
                                    .build());
            } catch (Exception e) {
                log.warn("Fabric network parse failed; skip: jsonNode={}", jsonNode);
            }
        });
        return fabricNetworks;
    }

    /**
     * Gets the set of configured local subnets.
     *
     * @return a set of subnets
     */
    public Set<FabricSubnet> fabricSubnets() {
        Set<FabricSubnet> subnets = Sets.newHashSet();
        JsonNode subnetsNode = object.get(FABRIC_SUBNETS);
        if (subnetsNode == null) {
            log.warn("FabricSubnets is null!");
            return subnets;
        }

        subnetsNode.forEach(jsonNode -> {
            String encapsulation = NONE_ENCAPSULATION;   // NONE or VLAN
            if (jsonNode.hasNonNull(ENCAPSULATION)) {
                encapsulation = jsonNode.get(ENCAPSULATION).asText();
            }
            try {
                subnets.add(DefaultFabricSubnet.builder()
                            .prefix(IpPrefix.valueOf(jsonNode.get(PREFIX).asText()))
                            .gatewayIp(IpAddress.valueOf(jsonNode.get(GATEWAY_IP).asText()))
                            .gatewayMac(MacAddress.valueOf(jsonNode.get(GATEWAY_MAC).asText()))
                            .encapsulation(EncapsulationType.enumFromString(encapsulation))
                            .networkName(jsonNode.get(NETWORK_NAME).asText())
                            .build());
            } catch (Exception e) {
                log.warn("Fabric subnet parse failed; skip: jsonNode={}", jsonNode);
            }
        });

        return subnets;
    }

    /**
     * Returns all routes in this configuration.
     *
     * @return a set of routes.
     */
    public Set<FabricRoute> fabricRoutes() {
        Set<FabricRoute> routes = Sets.newHashSet();

        JsonNode routesNode = object.get(FABRIC_ROUTES);
        if (routesNode == null) {
            return routes;
        }

        routesNode.forEach(jsonNode -> {
            try {
                routes.add(DefaultFabricRoute.builder()
                        .source(FabricRoute.Source.STATIC)
                        .prefix(IpPrefix.valueOf(jsonNode.path(PREFIX).asText()))
                        .nextHop(IpAddress.valueOf(jsonNode.path(NEXT_HOP).asText()))
                        .build());
            } catch (IllegalArgumentException e) {
                log.warn("Fabric router parse error; skip: jsonNode={}", jsonNode);
            }
        });

        return routes;
    }
}
