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

package org.onosproject.routing.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Routing configuration.
 *
 * @deprecated in Junco release. Use RoutersConfig instead.
 */
@Deprecated
public class RouterConfig extends Config<ApplicationId> {

    private static final String CP_CONNECT_POINT = "controlPlaneConnectPoint";
    private static final String OSPF_ENABLED = "ospfEnabled";
    private static final String PIM_ENABLED = "pimEnabled";
    private static final String INTERFACES = "interfaces";

    /**
     * Returns the routing control plane connect point.
     *
     * @return control plane connect point
     */
    public ConnectPoint getControlPlaneConnectPoint() {
        return ConnectPoint.deviceConnectPoint(object.path(CP_CONNECT_POINT).asText());
    }

    /**
     * Returns whether OSPF is enabled on this router.
     *
     * @return true if OSPF is enabled, otherwise false
     */
    public boolean getOspfEnabled() {
        return object.path(OSPF_ENABLED).asBoolean(false);
    }

    /**
     * Returns whether PIM is enabled on this router.
     *
     * @return true if PIM is enabled, otherwise false
     */
    public boolean pimEnabled() {
        return object.path(PIM_ENABLED).asBoolean(false);
    }

    /**
     * Returns the list of interfaces enabled on this router.
     *
     * @return list of interface names that are enabled, or an empty list if
     * all available interfaces should be used
     */
    public List<String> getInterfaces() {
        JsonNode intfNode = object.path(INTERFACES);
        if (intfNode.isMissingNode() || !intfNode.isArray()) {
            return Collections.emptyList();
        }
        ArrayNode array = (ArrayNode) intfNode;
        List<String> interfaces = new ArrayList<>(array.size());
        for (JsonNode intf : array) {
            interfaces.add(intf.asText());
        }
        return interfaces;
    }

    @Override
    public boolean isValid() {
        if (!hasOnlyFields(INTERFACES, CP_CONNECT_POINT, OSPF_ENABLED, PIM_ENABLED)) {
            return false;
        }

        JsonNode intfNode = object.path(INTERFACES);
        if (!intfNode.isMissingNode() && !intfNode.isArray()) {
            return false;
        }

        return isConnectPoint(CP_CONNECT_POINT, FieldPresence.MANDATORY) &&
                isBoolean(OSPF_ENABLED, FieldPresence.OPTIONAL) &&
                isBoolean(PIM_ENABLED, FieldPresence.OPTIONAL);
    }
}
