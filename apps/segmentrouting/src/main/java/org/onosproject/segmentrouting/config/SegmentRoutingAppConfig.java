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

package org.onosproject.segmentrouting.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;

import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * App configuration object for Segment Routing.
 */
public class SegmentRoutingAppConfig extends Config<ApplicationId> {
    private static final String VROUTER_MACS = "vRouterMacs";
    private static final String SUPPRESS_SUBNET = "suppressSubnet";
    private static final String SUPPRESS_HOST_BY_PORT = "suppressHostByPort";
    // TODO We might want to move SUPPRESS_HOST_BY_PROVIDER to Component Config
    private static final String SUPPRESS_HOST_BY_PROVIDER = "suppressHostByProvider";
    private static final String MPLS_ECMP = "MPLS-ECMP";

    @Override
    public boolean isValid() {
        return hasOnlyFields(VROUTER_MACS, SUPPRESS_SUBNET,
                SUPPRESS_HOST_BY_PORT, SUPPRESS_HOST_BY_PROVIDER, MPLS_ECMP) &&
                vRouterMacs() != null &&
                suppressSubnet() != null && suppressHostByPort() != null &&
                suppressHostByProvider() != null;
    }

    /**
     * Gets MPLS-ECMP configuration from the config.
     *
     * @return the configuration of MPLS-ECMP. If it is not
     *         specified, the default behavior is false.
     */
    public boolean mplsEcmp() {
        return get(MPLS_ECMP, false);
    }

    /**
     * Sets MPLS-ECMP to the config.
     *
     * @param mplsEcmp the MPLS-ECMP configuration
     * @return this {@link SegmentRoutingAppConfig}
     */
    public SegmentRoutingAppConfig setMplsEcmp(boolean mplsEcmp) {
        object.put(MPLS_ECMP, mplsEcmp);
        return this;
    }

    /**
     * Gets vRouters from the config.
     *
     * @return Set of vRouter MAC addresses, empty is not specified,
     *         or null if not valid
     */
    public Set<MacAddress> vRouterMacs() {
        if (!object.has(VROUTER_MACS)) {
            return ImmutableSet.of();
        }

        ImmutableSet.Builder<MacAddress> builder = ImmutableSet.builder();
        ArrayNode arrayNode = (ArrayNode) object.path(VROUTER_MACS);
        for (JsonNode jsonNode : arrayNode) {
            MacAddress mac;

            String macStr = jsonNode.asText(null);
            if (macStr == null) {
                return null;
            }
            try {
                mac = MacAddress.valueOf(macStr);
            } catch (IllegalArgumentException e) {
                return null;
            }

            builder.add(mac);
        }
        return builder.build();
    }

    /**
     * Sets vRouters to the config.
     *
     * @param vRouterMacs a set of vRouter MAC addresses
     * @return this {@link SegmentRoutingAppConfig}
     */
    public SegmentRoutingAppConfig setVRouterMacs(Set<MacAddress> vRouterMacs) {
        if (vRouterMacs == null) {
            object.remove(VROUTER_MACS);
        } else {
            ArrayNode arrayNode = mapper.createArrayNode();

            vRouterMacs.forEach(mac -> {
                arrayNode.add(mac.toString());
            });

            object.set(VROUTER_MACS, arrayNode);
        }
        return this;
    }

    /**
     * Gets names of ports to which SegmentRouting does not push subnet rules.
     *
     * @return Set of port names, empty if not specified, or null
     *         if not valid
     */
    public Set<ConnectPoint> suppressSubnet() {
        if (!object.has(SUPPRESS_SUBNET)) {
            return ImmutableSet.of();
        }

        ImmutableSet.Builder<ConnectPoint> builder = ImmutableSet.builder();
        ArrayNode arrayNode = (ArrayNode) object.path(SUPPRESS_SUBNET);
        for (JsonNode jsonNode : arrayNode) {
            String portName = jsonNode.asText(null);
            if (portName == null) {
                return null;
            }
            try {
                builder.add(ConnectPoint.deviceConnectPoint(portName));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return builder.build();
    }

    /**
     * Sets names of ports to which SegmentRouting does not push subnet rules.
     *
     * @param suppressSubnet names of ports to which SegmentRouting does not push
     *                     subnet rules
     * @return this {@link SegmentRoutingAppConfig}
     */
    public SegmentRoutingAppConfig setSuppressSubnet(Set<ConnectPoint> suppressSubnet) {
        if (suppressSubnet == null) {
            object.remove(SUPPRESS_SUBNET);
        } else {
            ArrayNode arrayNode = mapper.createArrayNode();
            suppressSubnet.forEach(connectPoint -> {
                arrayNode.add(connectPoint.deviceId() + "/" + connectPoint.port());
            });
            object.set(SUPPRESS_SUBNET, arrayNode);
        }
        return this;
    }

    /**
     * Gets connect points to which SegmentRouting does not push host rules.
     *
     * @return Set of connect points, empty if not specified, or null
     *         if not valid
     */
    public Set<ConnectPoint> suppressHostByPort() {
        if (!object.has(SUPPRESS_HOST_BY_PORT)) {
            return ImmutableSet.of();
        }

        ImmutableSet.Builder<ConnectPoint> builder = ImmutableSet.builder();
        ArrayNode arrayNode = (ArrayNode) object.path(SUPPRESS_HOST_BY_PORT);
        for (JsonNode jsonNode : arrayNode) {
            String portName = jsonNode.asText(null);
            if (portName == null) {
                return null;
            }
            try {
                builder.add(ConnectPoint.deviceConnectPoint(portName));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return builder.build();
    }

    /**
     * Sets connect points to which SegmentRouting does not push host rules.
     *
     * @param connectPoints connect points to which SegmentRouting does not push
     *                     host rules
     * @return this {@link SegmentRoutingAppConfig}
     */
    public SegmentRoutingAppConfig setSuppressHostByPort(Set<ConnectPoint> connectPoints) {
        if (connectPoints == null) {
            object.remove(SUPPRESS_HOST_BY_PORT);
        } else {
            ArrayNode arrayNode = mapper.createArrayNode();
            connectPoints.forEach(connectPoint -> {
                arrayNode.add(connectPoint.deviceId() + "/" + connectPoint.port());
            });
            object.set(SUPPRESS_HOST_BY_PORT, arrayNode);
        }
        return this;
    }

    /**
     * Gets provider names from which SegmentRouting does not learn host info.
     *
     * @return array of provider names that need to be ignored
     */
    public Set<String> suppressHostByProvider() {
        if (!object.has(SUPPRESS_HOST_BY_PROVIDER)) {
            return ImmutableSet.of();
        }

        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        ArrayNode arrayNode = (ArrayNode) object.path(SUPPRESS_HOST_BY_PROVIDER);
        for (JsonNode jsonNode : arrayNode) {
            String providerName = jsonNode.asText(null);
            if (providerName == null) {
                return null;
            }
            builder.add(providerName);
        }
        return builder.build();
    }

    /**
     * Sets provider names from which SegmentRouting does not learn host info.
     *
     * @param providers set of provider names
     * @return this {@link SegmentRoutingAppConfig}
     */
    public SegmentRoutingAppConfig setSuppressHostByProvider(Set<String> providers) {
        if (providers == null) {
            object.remove(SUPPRESS_HOST_BY_PROVIDER);
        } else {
            ArrayNode arrayNode = mapper.createArrayNode();
            providers.forEach(arrayNode::add);
            object.set(SUPPRESS_HOST_BY_PROVIDER, arrayNode);
        }
        return this;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("vRouterMacs", vRouterMacs())
                .add("suppressSubnet", suppressSubnet())
                .add("suppressHostByPort", suppressHostByPort())
                .add("suppressHostByProvider", suppressHostByProvider())
                .add("mplsEcmp", mplsEcmp())
                .toString();
    }
}
