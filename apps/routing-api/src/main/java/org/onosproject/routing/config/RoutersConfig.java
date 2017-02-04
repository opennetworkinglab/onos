/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.routing.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;

import java.util.HashSet;
import java.util.Set;

/**
 * Routing configuration.
 */
public class RoutersConfig extends Config<ApplicationId> {

    private static final String CP_CONNECT_POINT = "controlPlaneConnectPoint";
    private static final String OSPF_ENABLED = "ospfEnabled";
    private static final String PIM_ENABLED = "pimEnabled";
    private static final String INTERFACES = "interfaces";

    /**
     * Gets the router configurations.
     *
     * @return set of router configurations
     */
    public Set<Router> getRouters() {
        Set<Router> routers = new HashSet<>();

        for (JsonNode routerNode : array) {
            ConnectPoint connectPoint = ConnectPoint.deviceConnectPoint(routerNode.path(CP_CONNECT_POINT).asText());
            boolean ospfEnabled = routerNode.path(OSPF_ENABLED).asBoolean();


            JsonNode intfNode = routerNode.path(INTERFACES);
            Set<String> interfaces = new HashSet<>(array.size());
            if (!intfNode.isMissingNode()) {
                ArrayNode array = (ArrayNode) intfNode;
                for (JsonNode intf : array) {
                    interfaces.add(intf.asText());
                }
            }

            routers.add(new Router(connectPoint, ospfEnabled, interfaces));
        }

        return ImmutableSet.copyOf(routers);
    }

    @Override
    public boolean isValid() {
        for (JsonNode node : array) {
            ObjectNode routerNode = (ObjectNode) node;
            if (!hasOnlyFields(routerNode, INTERFACES, CP_CONNECT_POINT, OSPF_ENABLED, PIM_ENABLED)) {
                return false;
            }

            JsonNode intfNode = routerNode.path(INTERFACES);
            if (!intfNode.isMissingNode() && !intfNode.isArray()) {
                return false;
            }

            boolean valid = isConnectPoint(routerNode, CP_CONNECT_POINT, FieldPresence.MANDATORY) &&
                    isBoolean(routerNode, OSPF_ENABLED, FieldPresence.OPTIONAL) &&
                    isBoolean(routerNode, PIM_ENABLED, FieldPresence.OPTIONAL);

            if (!valid) {
                return false;
            }
        }

        return true;
    }

    /**
     * Router configuration.
     */
    public static class Router {
        private final ConnectPoint connectPoint;
        private final boolean ospfEnabled;
        private final Set<String> interfaces;

        Router(ConnectPoint controlPlaneConnectPoint, boolean ospfEnabled, Set<String> interfaces) {
            this.connectPoint = controlPlaneConnectPoint;
            this.ospfEnabled = ospfEnabled;
            this.interfaces = interfaces;
        }

        /**
         * Returns the routing control plane connect point.
         *
         * @return control plane connect point
         */
        public ConnectPoint controlPlaneConnectPoint() {
            return connectPoint;
        }

        /**
         * Returns whether OSPF is enabled on this router.
         *
         * @return true if OSPF is enabled, otherwise false
         */
        public boolean isOspfEnabled() {
            return ospfEnabled;
        }

        /**
         * Returns the set of interfaces enabled on this router.
         *
         * @return set of interface names that are enabled, or an empty list if
         * all available interfaces should be used
         */
        public Set<String> interfaces() {
            return interfaces;
        }
    }
}
