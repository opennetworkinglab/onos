/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.config.Config;

import java.util.Set;

/**
 * Represents the VPLS application configuration.
 */
public class VplsAppConfig extends Config<ApplicationId> {
    private static final String VPLS = "vplsList";
    private static final String NAME = "name";
    private static final String INTERFACE = "interfaces";
    private static final String ENCAPSULATION = "encapsulation";

    // Record update time when VPLS distribute store update it
    private static final String UPDATE_TIME = "lastUpdateTime";

    /**
     * Returns a set of configured VPLSs.
     *
     * @return set of VPLSs
     */
    public Set<VplsConfig> vplss() {
        Set<VplsConfig> vplss = Sets.newHashSet();
        JsonNode vplsNode = object.get(VPLS);
        if (vplsNode == null) {
            return vplss;
        }

        vplsNode.forEach(jsonNode -> {
            String name = jsonNode.get(NAME).asText();

            Set<String> ifaces = Sets.newHashSet();
            JsonNode vplsIfaces = jsonNode.path(INTERFACE);
            if (vplsIfaces.toString().isEmpty()) {
                vplsIfaces = ((ObjectNode) jsonNode).putArray(INTERFACE);
            }
            vplsIfaces.forEach(ifacesNode -> ifaces.add(ifacesNode.asText()));

            String encap = null;
            if (jsonNode.hasNonNull(ENCAPSULATION)) {
                encap = jsonNode.get(ENCAPSULATION).asText();
            }
            vplss.add(new VplsConfig(name,
                                     ifaces,
                                     EncapsulationType.enumFromString(encap)));
        });
        return vplss;
    }

    /**
     * Returns the VPLS configuration given a VPLS name.
     *
     * @param name the name of the VPLS
     * @return the VPLS configuration if it exists; null otherwise
     */
    public VplsConfig getVplsWithName(String name) {
        return vplss().stream()
                      .filter(vpls -> vpls.name().equals(name))
                      .findFirst()
                      .orElse(null);
    }

    /**
     * Adds a VPLS to the configuration.
     *
     * @param vpls the name of the VPLS
     */
    public void addVpls(VplsConfig vpls) {
        ObjectNode vplsNode = JsonNodeFactory.instance.objectNode();

        vplsNode.put(NAME, vpls.name());

        ArrayNode ifacesNode = vplsNode.putArray(INTERFACE);
        vpls.ifaces().forEach(ifacesNode::add);

        vplsNode.put(ENCAPSULATION, vpls.encap().toString());

        ArrayNode vplsArray = vplss().isEmpty() ?
                initVplsConfiguration() : (ArrayNode) object.get(VPLS);
        vplsArray.add(vplsNode);
    }

    /**
     * Removes a VPLS from the configuration.
     *
     * @param vplsName the vplsName of the VPLS to be removed
     */
    public void removeVpls(String vplsName) {
        ArrayNode configuredVpls = (ArrayNode) object.get(VPLS);

        for (int i = 0; i < configuredVpls.size(); i++) {
            if (configuredVpls.get(i).hasNonNull(NAME) &&
                    configuredVpls.get(i).get(NAME).asText().equals(vplsName)) {
                configuredVpls.remove(i);
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
    public VplsConfig vplsFromIface(String iface) {
        for (VplsConfig vpls : vplss()) {
            if (vpls.isAttached(iface)) {
                return vpls;
            }
        }
        return null;
    }

    /**
     * Adds a network interface to a VPLS.
     *
     * @param vplsName the vplsName of the VPLS
     * @param iface the network interface to be added
     */
    public void addIface(String vplsName, String iface) {
        JsonNode vplsNodes = object.get(VPLS);
        vplsNodes.forEach(vplsNode -> {
            if (hasNamedNode(vplsNode, vplsName)) {
                ArrayNode ifacesNode = (ArrayNode) vplsNode.get(INTERFACE);
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
    public void removeIface(VplsConfig name, String iface) {
        JsonNode vplsNodes = object.get(VPLS);
        vplsNodes.forEach(vplsNode -> {
            if (hasNamedNode(vplsNode, name.name())) {
                ArrayNode ifacesNode = (ArrayNode) vplsNode.get(INTERFACE);
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
     * Activate, deactivates, sets the encapsulation type for a given VPLS.
     *
     * @param vplsName the vplsName of the VPLS
     * @param encap the encapsulation type, if set
     */
    public void setEncap(String vplsName, EncapsulationType encap) {
        JsonNode vplsNodes = object.get(VPLS);
        vplsNodes.forEach(vplsNode -> {
            if (hasNamedNode(vplsNode, vplsName)) {
                ((ObjectNode) vplsNode).put(ENCAPSULATION, encap.toString());
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

    /**
     * Sets the last time of update for the VPLS information in the store.
     *
     * @param timestamp the update time
     */
    public void updateTime(long timestamp) {
        object.put(UPDATE_TIME, timestamp);
    }

    /**
     * Retrieves the last time of update for the VPLS information in the store.
     *
     * @return update time, -1 if there is no update time in the config
     */
    public long updateTime() {
        if (object.get(UPDATE_TIME) != null) {
            return object.get(UPDATE_TIME).asLong();
        } else {
            // No update time
            return -1L;
        }
    }

    /**
     * Clears all VPLS configurations.
     */
    public void clearVplsConfig() {
        if (object.get(VPLS) != null) {
            object.remove(VPLS);
        }
        initVplsConfiguration();
    }
}
