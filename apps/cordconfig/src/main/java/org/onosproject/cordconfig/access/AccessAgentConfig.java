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

package org.onosproject.cordconfig.access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;

import java.util.Map;
import java.util.Optional;

import static org.onosproject.net.config.Config.FieldPresence.MANDATORY;
import static org.onosproject.net.config.Config.FieldPresence.OPTIONAL;

/**
 * Represents configuration for an OLT agent.
 */
public class AccessAgentConfig extends Config<DeviceId> {

    private static final String OLTS = "olts";
    private static final String AGENT_MAC = "mac";

    // TODO: Remove this, it is only useful as long as XOS doesn't manage this.
    private static final String VTN_LOCATION = "vtn-location";

    @Override
    public boolean isValid() {
        return hasOnlyFields(OLTS, AGENT_MAC, VTN_LOCATION) &&
                isMacAddress(AGENT_MAC, MANDATORY) &&
                isConnectPoint(VTN_LOCATION, OPTIONAL) &&
                isValidOlts();
    }

    /**
     * Gets the access agent configuration for this device.
     *
     * @return access agent configuration
     */
    public AccessAgentData getAgent() {
        JsonNode olts = node.get(OLTS);
        Map<ConnectPoint, MacAddress> oltMacInfo = Maps.newHashMap();
        olts.fields().forEachRemaining(item -> oltMacInfo.put(
                new ConnectPoint(subject(), PortNumber.fromString(item.getKey())),
                MacAddress.valueOf(item.getValue().asText())));

        MacAddress agentMac = MacAddress.valueOf(node.path(AGENT_MAC).asText());

        JsonNode vtn = node.path(VTN_LOCATION);
        Optional<ConnectPoint> vtnLocation;
        if (vtn.isMissingNode()) {
            vtnLocation = Optional.empty();
        } else {
            vtnLocation = Optional.of(ConnectPoint.deviceConnectPoint(vtn.asText()));
        }

        return new AccessAgentData(subject(), oltMacInfo, agentMac, vtnLocation);
    }

    private boolean isValidOlts() {
        JsonNode olts = node.get(OLTS);
        if (!olts.isObject()) {
            return false;
        }
        return !Iterators.any(olts.fields(), item -> !StringUtils.isNumeric(item.getKey()) ||
                        !isMacAddress((ObjectNode) olts, item.getKey(), MANDATORY));
    }
}
