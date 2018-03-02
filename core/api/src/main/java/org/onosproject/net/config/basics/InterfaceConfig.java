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

package org.onosproject.net.config.basics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigException;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intf.Interface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration for interfaces.
 */
@Beta
public class InterfaceConfig extends Config<ConnectPoint> {
    private static Logger log = LoggerFactory.getLogger(InterfaceConfig.class);

    public static final String NAME = "name";
    public static final String IPS = "ips";
    public static final String MAC = "mac";
    public static final String VLAN = "vlan";
    public static final String VLAN_UNTAGGED = "vlan-untagged";
    public static final String VLAN_TAGGED = "vlan-tagged";
    public static final String VLAN_NATIVE = "vlan-native";

    private static final String CONFIG_VALUE_ERROR = "Error parsing config value";
    private static final String INTF_NULL_ERROR = "Interface cannot be null";
    private static final String INTF_NAME_ERROR = "Interface must have a valid name";

    @Override
    public boolean isValid() {
        for (JsonNode node : array) {
            if (!hasOnlyFields((ObjectNode) node, NAME, IPS, MAC, VLAN,
                    VLAN_UNTAGGED, VLAN_TAGGED, VLAN_NATIVE)) {
                return false;
            }

            ObjectNode obj = (ObjectNode) node;

            if (!(isString(obj, NAME, FieldPresence.OPTIONAL) &&
                    isMacAddress(obj, MAC, FieldPresence.OPTIONAL) &&
                    isIntegralNumber(obj, VLAN, FieldPresence.OPTIONAL, 0, VlanId.MAX_VLAN) &&
                    isIntegralNumber(obj, VLAN_UNTAGGED, FieldPresence.OPTIONAL, 0, VlanId.MAX_VLAN) &&
                    isIntegralNumber(obj, VLAN_NATIVE, FieldPresence.OPTIONAL, 0, VlanId.MAX_VLAN))) {
                return false;
            }

            for (JsonNode ipNode : node.path(IPS)) {
                if (!ipNode.isTextual() || IpPrefix.valueOf(ipNode.asText()) == null) {
                    return false;
                }
            }

            checkArgument(!hasField(obj, VLAN_TAGGED) ||
                            (node.path(VLAN_TAGGED).isArray() && node.path(VLAN_TAGGED).size() >= 1),
                    "%s must be an array with at least one element", VLAN_TAGGED);

            for (JsonNode vlanNode : node.path(VLAN_TAGGED)) {
                checkArgument(vlanNode.isInt() &&
                        vlanNode.intValue() >= 0 &&  vlanNode.intValue() <= VlanId.MAX_VLAN,
                        "Invalid VLAN ID %s in %s", vlanNode.intValue(), VLAN_TAGGED);
            }

            checkArgument(!hasField(obj, VLAN_UNTAGGED) ||
                    !(hasField(obj, VLAN_TAGGED) || hasField(obj, VLAN_NATIVE)),
                    "%s and %s should not be used when %s is set", VLAN_TAGGED, VLAN_NATIVE, VLAN_UNTAGGED);

            checkArgument(!hasField(obj, VLAN_TAGGED) || !hasField(obj, VLAN_UNTAGGED),
                    "%s should not be used when %s is set", VLAN_UNTAGGED, VLAN_TAGGED);

            checkArgument(!hasField(obj, VLAN_NATIVE) || hasField(obj, VLAN_TAGGED),
                    "%s should not be used alone without %s", VLAN_NATIVE, VLAN_TAGGED);

            checkArgument(!hasField(obj, VLAN_NATIVE) || !hasField(obj, VLAN_TAGGED) ||
                    !getVlans(obj, VLAN_TAGGED).contains(getVlan(obj, VLAN_NATIVE)),
                    "%s cannot be one of the VLANs configured in %s", VLAN_NATIVE, VLAN_TAGGED);
        }
        return true;
    }

    /**
     * Retrieves all interfaces configured on this port.
     *
     * @return set of interfaces
     * @throws ConfigException if there is any error in the JSON config
     */
    public Set<Interface> getInterfaces() throws ConfigException {
        Set<Interface> interfaces = Sets.newHashSet();

        try {
            for (JsonNode intfNode : array) {
                String name = intfNode.path(NAME).asText(null);

                List<InterfaceIpAddress> ips = getIps(intfNode);

                String mac = intfNode.path(MAC).asText();
                MacAddress macAddr = mac.isEmpty() ? null : MacAddress.valueOf(mac);

                VlanId vlan = getVlan(intfNode, VLAN);
                VlanId vlanUntagged = getVlan(intfNode, VLAN_UNTAGGED);
                Set<VlanId> vlanTagged = getVlans(intfNode, VLAN_TAGGED);
                VlanId vlanNative = getVlan(intfNode, VLAN_NATIVE);

                interfaces.add(new Interface(name, subject, ips, macAddr, vlan,
                        vlanUntagged, vlanTagged, vlanNative));
            }
        } catch (IllegalArgumentException e) {
            throw new ConfigException(CONFIG_VALUE_ERROR, e);
        }

        return interfaces;
    }

    /**
     * Adds an interface to the config.
     *
     * @param intf interface to add
     */
    public void addInterface(Interface intf) {
        checkNotNull(intf, INTF_NULL_ERROR);
        checkArgument(!intf.name().equals(Interface.NO_INTERFACE_NAME), INTF_NAME_ERROR);

        // Remove old interface with this name if it exists
        removeInterface(intf.name());

        ObjectNode intfNode = array.addObject();

        intfNode.put(NAME, intf.name());

        if (intf.mac() != null) {
            intfNode.put(MAC, intf.mac().toString());
        }

        if (!intf.ipAddressesList().isEmpty()) {
            intfNode.set(IPS, putIps(intf.ipAddressesList()));
        }

        if (!intf.vlan().equals(VlanId.NONE)) {
            intfNode.put(VLAN, intf.vlan().toString());
        }

        if (!intf.vlanUntagged().equals(VlanId.NONE)) {
            intfNode.put(VLAN_UNTAGGED, intf.vlanUntagged().toString());
        }

        if (!intf.vlanTagged().isEmpty()) {
            intfNode.set(VLAN_TAGGED, putVlans(intf.vlanTagged()));
        }

        if (!intf.vlanNative().equals(VlanId.NONE)) {
            intfNode.put(VLAN_NATIVE, intf.vlanNative().toString());
        }
    }

    /**
     * Removes an interface from the config.
     *
     * @param name name of the interface to remove
     */
    public void removeInterface(String name) {
        checkNotNull(name, INTF_NULL_ERROR);
        checkArgument(!name.equals(Interface.NO_INTERFACE_NAME), INTF_NAME_ERROR);

        Iterator<JsonNode> it = array.iterator();
        while (it.hasNext()) {
            JsonNode node = it.next();
            if (node.path(NAME).asText().equals(name)) {
                it.remove();
                break;
            }
        }
    }

    /**
     * Extracts VLAN ID from given path of given json node.
     *
     * @param node JSON node
     * @param path path
     * @return VLAN ID
     */
    private VlanId getVlan(JsonNode node, String path) {
        VlanId vlan = VlanId.NONE;
        if (!node.path(path).isMissingNode()) {
            vlan = VlanId.vlanId(Short.valueOf(node.path(path).asText()));
        }
        return vlan;
    }

    /**
     * Extracts a set of VLAN ID from given path of given json node.
     *
     * @param node JSON node
     * @param path path
     * @return a set of VLAN ID
     */
    private Set<VlanId> getVlans(JsonNode node, String path) {
        ImmutableSet.Builder<VlanId> vlanIdBuilder = ImmutableSet.builder();

        JsonNode vlansNode = node.get(path);
        if (vlansNode != null) {
            vlansNode.forEach(vlanNode ->
                    vlanIdBuilder.add(VlanId.vlanId(vlanNode.shortValue())));
        }

        return vlanIdBuilder.build();
    }

    private ArrayNode putVlans(Set<VlanId> vlans) {
        ArrayNode vlanArray = mapper.createArrayNode();

        vlans.forEach(vlan -> vlanArray.add(vlan.toShort()));

        return vlanArray;
    }

    private List<InterfaceIpAddress> getIps(JsonNode node) {
        List<InterfaceIpAddress> ips = Lists.newArrayList();

        JsonNode ipsNode = node.get(IPS);
        if (ipsNode != null) {
            ipsNode.forEach(jsonNode ->
                    ips.add(InterfaceIpAddress.valueOf(jsonNode.asText())));
        }

        return ips;
    }

    private ArrayNode putIps(List<InterfaceIpAddress> intfIpAddresses) {
        ArrayNode ipArray = mapper.createArrayNode();

        intfIpAddresses.forEach(i -> ipArray.add(i.toString()));

        return ipArray;
    }

}
