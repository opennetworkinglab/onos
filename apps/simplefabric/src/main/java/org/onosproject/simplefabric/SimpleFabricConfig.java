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

package org.onosproject.simplefabric;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.net.EncapsulationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Configuration object for prefix config.
 */
public class SimpleFabricConfig extends Config<ApplicationId> {
    public static final String KEY = "simpleFabric";

    private static final String L2NETWORKS = "l2Networks";
    private static final String NAME = "name";
    private static final String INTERFACES = "interfaces";
    private static final String ENCAPSULATION = "encapsulation";
    private static final String L2FORWARD = "l2Forward";
    private static final String L2BROADCAST = "l2Broadcast";
    private static final String IPSUBNETS = "ipSubnets";
    private static final String BORDERROUTES = "borderRoutes";
    private static final String IPPREFIX = "ipPrefix";
    private static final String GATEWAYIP = "gatewayIp";
    private static final String GATEWAYMAC = "gatewayMac";
    private static final String L2NETWORKNAME = "l2NetworkName";
    private static final String NEXTHOP = "nextHop";

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Returns all l2Networks in this configuration.
     *
     * @return A set of L2Network.
     */
    public Set<L2Network> getL2Networks() {
        Set<L2Network> l2Networks = Sets.newHashSet();
        JsonNode l2NetworkNode = object.get(L2NETWORKS);
        if (l2NetworkNode == null) {
            return l2Networks;
        }

        l2NetworkNode.forEach(jsonNode -> {
            Set<String> ifaces = Sets.newHashSet();
            JsonNode l2NetworkIfaces = jsonNode.path(INTERFACES);
            if (l2NetworkIfaces == null) {
                log.warn("simple fabric network config cannot find {}; skip: jsonNode={}", INTERFACES, jsonNode);
            } else if (!l2NetworkIfaces.toString().isEmpty()) {
                l2NetworkIfaces.forEach(ifacesNode -> ifaces.add(new String(ifacesNode.asText())));
            }
            String encapsulation = "NONE";   // NONE or VLAN
            if (jsonNode.hasNonNull(ENCAPSULATION)) {
                encapsulation = jsonNode.get(ENCAPSULATION).asText();
            }
            boolean l2Forward = true;
            if (jsonNode.hasNonNull(L2FORWARD)) {
                l2Forward = jsonNode.get(L2FORWARD).asBoolean();
            }
            boolean l2Broadcast = true;
            if (jsonNode.hasNonNull(L2BROADCAST)) {
                l2Broadcast = jsonNode.get(L2BROADCAST).asBoolean();
            }
            try {
                l2Networks.add(new L2Network(
                        jsonNode.get(NAME).asText(), ifaces, EncapsulationType.enumFromString(encapsulation),
                        l2Forward, l2Broadcast));
            } catch (Exception e) {
                log.warn("simple fabric network config l2Network parse failed; skip: error={} jsonNode={}", jsonNode);
            }
        });
        return l2Networks;
    }

    /**
     * Gets the set of configured local IP subnets.
     *
     * @return IP Subnets
     */
    public Set<IpSubnet> ipSubnets() {
        Set<IpSubnet> subnets = Sets.newHashSet();
        JsonNode subnetsNode = object.get(IPSUBNETS);
        if (subnetsNode == null) {
            log.warn("simple fabric network config ipSubnets is null!");
            return subnets;
        }

        subnetsNode.forEach(jsonNode -> {
            String encapsulation = "NONE";   // NONE or VLAN
            if (jsonNode.hasNonNull(ENCAPSULATION)) {
                encapsulation = jsonNode.get(ENCAPSULATION).asText();
            }
            try {
                subnets.add(new IpSubnet(
                        IpPrefix.valueOf(jsonNode.get(IPPREFIX).asText()),
                        IpAddress.valueOf(jsonNode.get(GATEWAYIP).asText()),
                        MacAddress.valueOf(jsonNode.get(GATEWAYMAC).asText()),
                        EncapsulationType.enumFromString(encapsulation),
                        jsonNode.get(L2NETWORKNAME).asText()));
            } catch (Exception e) {
                log.warn("simple fabric network config ipSubnet parse failed; skip: error={} jsonNode={}", jsonNode);
            }
        });

        return subnets;
    }

    /**
     * Returns all routes in this configuration.
     *
     * @return A set of route.
     */
    public Set<Route> borderRoutes() {
        Set<Route> routes = Sets.newHashSet();

        JsonNode routesNode = object.get(BORDERROUTES);
        if (routesNode == null) {
            //log.warn("simple fabric network config borderRoutes is null!");
            return routes;
        }

        routesNode.forEach(jsonNode -> {
            try {
                routes.add(new Route(
                      Route.Source.STATIC,
                      IpPrefix.valueOf(jsonNode.path(IPPREFIX).asText()),
                      IpAddress.valueOf(jsonNode.path(NEXTHOP).asText())));
            } catch (IllegalArgumentException e) {
                log.warn("simple fabric network config parse error; skip: {}", jsonNode);
            }
        });

        return routes;
    }

}
