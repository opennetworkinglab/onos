/*
 * Copyright 2016 Open Networking Laboratory
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
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * App configuration object for Segment Routing.
 */
public class SegmentRoutingAppConfig extends Config<ApplicationId> {
    private static final String VROUTER_MACS = "vRouterMacs";
    private static final String VROUTER_ID = "vRouterId";
    private static final String EXCLUDE_PORTS = "excludePorts";

    @Override
    public boolean isValid() {
        return hasOnlyFields(VROUTER_MACS, VROUTER_ID, EXCLUDE_PORTS) &&
                vRouterMacs() != null && vRouterId() != null &&
                excludePorts() != null;
    }

    /**
     * Gets vRouters from the config.
     *
     * @return a set of vRouter MAC addresses
     */
    public Set<MacAddress> vRouterMacs() {
        if (!object.has(VROUTER_MACS)) {
            return null;
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
     * Gets vRouter device ID.
     *
     * @return vRouter device ID, or null if not valid
     */
    public DeviceId vRouterId() {
        if (!object.has(VROUTER_ID)) {
            return null;
        }

        try {
            return DeviceId.deviceId(object.path(VROUTER_ID).asText());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Sets vRouter device ID.
     *
     * @param vRouterId vRouter device ID
     * @return this {@link SegmentRoutingAppConfig}
     */
    public SegmentRoutingAppConfig setVRouterId(DeviceId vRouterId) {
        if (vRouterId == null) {
            object.remove(VROUTER_ID);
        } else {
            object.put(VROUTER_ID, vRouterId.toString());
        }
        return this;
    }

    /**
     * Gets names of ports that are ignored by SegmentRouting.
     *
     * @return set of port names
     */
    public Set<String> excludePorts() {
        if (!object.has(EXCLUDE_PORTS)) {
            return null;
        }

        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        ArrayNode arrayNode = (ArrayNode) object.path(EXCLUDE_PORTS);
        for (JsonNode jsonNode : arrayNode) {
            String portName = jsonNode.asText(null);
            if (portName == null) {
                return null;
            }
            builder.add(portName);
        }
        return builder.build();
    }

    /**
     * Sets names of ports that are ignored by SegmentRouting.
     *
     * @param excludePorts names of ports that are ignored by SegmentRouting
     * @return this {@link SegmentRoutingAppConfig}
     */
    public SegmentRoutingAppConfig setExcludePorts(Set<String> excludePorts) {
        if (excludePorts == null) {
            object.remove(EXCLUDE_PORTS);
        } else {
            ArrayNode arrayNode = mapper.createArrayNode();
            excludePorts.forEach(portName -> {
                arrayNode.add(portName);
            });
            object.set(EXCLUDE_PORTS, arrayNode);
        }
        return this;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("vRouterMacs", vRouterMacs())
                .add("excludePorts", excludePorts())
                .toString();
    }
}
