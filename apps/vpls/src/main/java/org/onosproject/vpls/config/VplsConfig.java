/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.vpls.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;

import java.util.Set;

/**
 * Configuration object for VPLS config.
 */
public class VplsConfig extends Config<ApplicationId> {
    private static final String VPLS = "vplsNetworks";
    private static final String NAME = "name";
    private static final String INTERFACE = "interfaces";

    /**
     * Returns a set of configured VPLSs.
     *
     * @return set of VPLSs
     */
    public Set<VplsNetworkConfig> vplsNetworks() {
        Set<VplsNetworkConfig> vpls = Sets.newHashSet();

        JsonNode vplsNode = object.get(VPLS);

        if (vplsNode == null) {
            return vpls;
        }

        vplsNode.forEach(jsonNode -> {
            Set<String> ifaces = Sets.newHashSet();
            jsonNode.path(INTERFACE).forEach(ifacesNode ->
                    ifaces.add(ifacesNode.asText())
            );

            String name = jsonNode.get(NAME).asText();

            vpls.add(new VplsNetworkConfig(name, ifaces));
        });

        return vpls;
    }

    /**
     * Returns the VPLS configuration given a VPLS name.
     *
     * @param name the VPLS name
     * @return the VPLS configuration if it exists; null otherwise
     */
    public VplsNetworkConfig getVplsWithName(String name) {
        for (VplsNetworkConfig vpls : vplsNetworks()) {
            if (vpls.name().equals(name)) {
                return vpls;
            }
        }
        return null;
    }

    /**
     * Adds a VPLS to the configuration.
     *
     * @param name the name of the VPLS to be added
     */
    public void addVpls(VplsNetworkConfig name) {
        ObjectNode vplsNode = JsonNodeFactory.instance.objectNode();

        vplsNode.put(NAME, name.name());

        ArrayNode ifacesNode = vplsNode.putArray(INTERFACE);
        name.ifaces().forEach(ifacesNode::add);

        ArrayNode vplsArray = vplsNetworks().isEmpty() ?
                initVplsConfiguration() : (ArrayNode) object.get(VPLS);
        vplsArray.add(vplsNode);
    }

    /**
     * Removes a VPLS from the configuration.
     *
     * @param name the name of the VPLS to be removed
     */
    public void removeVpls(String name) {
        ArrayNode vplsArray = (ArrayNode) object.get(VPLS);

        for (int i = 0; i < vplsArray.size(); i++) {
            if (vplsArray.get(i).hasNonNull(NAME) &&
                    vplsArray.get(i).get(NAME).asText().equals(name)) {
                vplsArray.remove(i);
                return;
            }
        }
    }

    /**
     * Finds a VPLS with a given network interface.
     *
     * @param iface the network interface
     * @return the VPLS if found; null otherwise
     */
    public VplsNetworkConfig getVplsFromInterface(String iface) {
        for (VplsNetworkConfig vpls : vplsNetworks()) {
            if (vpls.isAttached(iface)) {
                return vpls;
            }
        }
        return null;
    }

    /**
     * Adds a network interface to a VPLS.
     *
     * @param name the name of the VPLS
     * @param iface the network interface to be added
     */
    public void addInterfaceToVpls(String name, String iface) {
        JsonNode vplsNode = object.get(VPLS);
        vplsNode.forEach(jsonNode -> {

            if (hasNamedNode(jsonNode, name)) {
                ArrayNode ifacesNode = (ArrayNode) jsonNode.get(INTERFACE);
                for (int i = 0; i < ifacesNode.size(); i++) {
                    if (ifacesNode.get(i).asText().equals(iface)) {
                        return; // Interface already exists.
                    }
                }
                ifacesNode.add(iface);
            }
        });
    }

    /**
     * Removes a network interface from a VPLS.
     *
     * @param name the name of the VPLS
     * @param iface the network interface to be removed
     */
    public void removeInterfaceFromVpls(VplsNetworkConfig name, String iface) {
        JsonNode vplsNode = object.get(VPLS);
        vplsNode.forEach(jsonNode -> {
            if (hasNamedNode(jsonNode, name.name())) {
                ArrayNode ifacesNode = (ArrayNode) jsonNode.get(INTERFACE);
                for (int i = 0; i < ifacesNode.size(); i++) {
                    if (ifacesNode.get(i).asText().equals(iface)) {
                        ifacesNode.remove(i);
                        return;
                    }
                }
            }
        });
    }

    /**
     * States if a JSON node has a "name" attribute and if the value is equal to
     * the name given.
     *
     * @param jsonNode the JSON node
     * @param name the node name
     * @return true if the JSON node has a "name" attribute with value equal to
     * the name given; false otherwise
     */
    private boolean hasNamedNode(JsonNode jsonNode, String name) {
        return jsonNode.hasNonNull(NAME) &&
                jsonNode.get(NAME).asText().equals(name);
    }

    /**
     * Creates an empty VPLS configuration.
     *
     * @return empty ArrayNode to store the VPLS configuration
     */
    private ArrayNode initVplsConfiguration() {
        return object.putArray(VPLS);
    }
}
