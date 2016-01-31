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
import org.onosproject.net.config.Config;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * App configuration object for Segment Routing.
 */
public class SegmentRoutingAppConfig extends Config<ApplicationId> {
    private static final String VROUTER_MACS = "vRouterMacs";

    @Override
    public boolean isValid() {
        return hasOnlyFields(VROUTER_MACS) && vRouterMacs() != null;
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

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("vRouterMacs", vRouterMacs())
                .toString();
    }
}
