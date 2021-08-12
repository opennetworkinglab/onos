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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onlab.packet.IpAddress;
import org.onosproject.kubevirtnetworking.api.KubevirtHostRoute;
import org.onosproject.kubevirtnetworking.api.KubevirtIpPool;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;

/**
 * Hamcrest matcher for kubevirt network.
 */
public final class KubevirtNetworkJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final KubevirtNetwork network;
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

    private KubevirtNetworkJsonMatcher(KubevirtNetwork network) {
        this.network = network;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {
        // check network ID
        String jsonNetworkId = jsonNode.get(NETWORK_ID).asText();
        String networkId = network.networkId();
        if (!jsonNetworkId.equals(networkId)) {
            description.appendText("network ID was " + jsonNetworkId);
            return false;
        }

        // check type
        String jsonType = jsonNode.get(TYPE).asText();
        String type = network.type().name();
        if (!jsonType.equals(type)) {
            description.appendText("network type was " + jsonType);
            return false;
        }

        // check name
        String jsonName = jsonNode.get(NAME).asText();
        String name = network.name();
        if (!jsonName.equals(name)) {
            description.appendText("network name was " + jsonName);
            return false;
        }

        // check MTU
        int jsonMtu = jsonNode.get(MTU).asInt();
        int mtu = network.mtu();
        if (jsonMtu != mtu) {
            description.appendText("network MTU was " + jsonMtu);
            return false;
        }

        // check gateway IP
        String jsonGatewayIp = jsonNode.get(GATEWAY_IP).asText();
        String gatewayIp = network.gatewayIp().toString();
        if (!jsonGatewayIp.equals(gatewayIp)) {
            description.appendText("gateway IP was " + jsonGatewayIp);
            return false;
        }

        // check default route
        boolean jsonDefaultRoute = jsonNode.get(DEFAULT_ROUTE).asBoolean();
        boolean defaultRoute = network.defaultRoute();
        if (jsonDefaultRoute != defaultRoute) {
            description.appendText("Default route was " + jsonDefaultRoute);
            return false;
        }

        // check CIDR
        String jsonCidr = jsonNode.get(CIDR).asText();
        String cidr = network.cidr();
        if (!jsonCidr.equals(cidr)) {
            description.appendText("CIDR was " + jsonCidr);
            return false;
        }

        // check segment ID
        JsonNode jsonSegmentId = jsonNode.get(SEGMENT_ID);
        if (jsonSegmentId != null) {
            String segmentId = network.segmentId();
            if (!jsonSegmentId.asText().equals(segmentId)) {
                description.appendText("segment ID was " + jsonSegmentId.asText());
                return false;
            }
        }

        // check ip pool
        JsonNode jsonIpPool = jsonNode.get(IP_POOL);
        if (jsonIpPool != null) {
            KubevirtIpPool ipPool = network.ipPool();
            KubevirtIpPoolJsonMatcher ipPoolMatcher =
                    KubevirtIpPoolJsonMatcher.matchesKubevirtIpPool(ipPool);
            if (ipPoolMatcher.matches(jsonIpPool)) {
                return true;
            } else {
                description.appendText("IP pool was " + jsonIpPool.toString());
                return false;
            }
        }

        // check host routes
        JsonNode jsonHostRoutes = jsonNode.get(HOST_ROUTES);
        if (jsonHostRoutes != null) {
            if (jsonHostRoutes.size() != network.hostRoutes().size()) {
                description.appendText("host routes size was " + jsonHostRoutes.size());
                return false;
            }

            for (KubevirtHostRoute hostRoute : network.hostRoutes()) {
                boolean routeFound = false;
                for (int routeIndex = 0; routeIndex < jsonHostRoutes.size(); routeIndex++) {
                    KubevirtHostRouteJsonMatcher routeMatcher =
                            KubevirtHostRouteJsonMatcher.matchesKubevirtHostRoute(hostRoute);
                    if (routeMatcher.matches(jsonHostRoutes.get(routeIndex))) {
                        routeFound = true;
                        break;
                    }
                }

                if (!routeFound) {
                    description.appendText("Host route not found " + hostRoute.toString());
                    return false;
                }
            }
        }

        // check dnses
        JsonNode jsonDnses = jsonNode.get(DNSES);
        if (jsonDnses != null) {
            if (jsonDnses.size() != network.dnses().size()) {
                description.appendText("DNSes size was " + jsonDnses.size());
                return false;
            }


            for (IpAddress dns : network.dnses()) {
                boolean dnsFound = false;
                for (int dnsIndex = 0; dnsIndex < jsonDnses.size(); dnsIndex++) {
                    String jsonDns = jsonDnses.get(dnsIndex).asText();
                    if (jsonDns.equals(dns.toString())) {
                        dnsFound = true;
                        break;
                    }
                }

                if (!dnsFound) {
                    description.appendText("DNS not found " + dns.toString());
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(network.toString());
    }

    /**
     * Factory to allocate an kubevirt network matcher.
     *
     * @param network kubevirt network object we are looking for
     * @return matcher
     */
    public static KubevirtNetworkJsonMatcher
                    matchesKubevirtNetwork(KubevirtNetwork network) {
        return new KubevirtNetworkJsonMatcher(network);
    }
}
