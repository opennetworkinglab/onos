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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.segmentrouting.storekey.XConnectStoreKey;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Configuration object for cross-connect.
 */
public class XConnectConfig extends Config<ApplicationId> {
    private static final String VLAN = "vlan";
    private static final String PORTS = "ports";
    private static final String NAME = "name"; // dummy field for naming

    private static final String UNEXPECTED_FIELD_NAME = "Unexpected field name";

    @Override
    public boolean isValid() {
        try {
            getXconnects().forEach(this::getPorts);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /**
     * Returns all xconnect keys.
     *
     * @return all keys (device/vlan pairs)
     * @throws IllegalArgumentException if wrong format
     */
    public Set<XConnectStoreKey> getXconnects() {
        ImmutableSet.Builder<XConnectStoreKey> builder = ImmutableSet.builder();
        object.fields().forEachRemaining(entry -> {
            DeviceId deviceId = DeviceId.deviceId(entry.getKey());
            builder.addAll(getXconnects(deviceId));
        });
        return builder.build();
    }

    /**
     * Returns xconnect keys of given device.
     *
     * @param deviceId ID of the device from which we want to get XConnect info
     * @return xconnect keys (device/vlan pairs) of given device
     * @throws IllegalArgumentException if wrong format
     */
    public Set<XConnectStoreKey> getXconnects(DeviceId deviceId) {
        ImmutableSet.Builder<XConnectStoreKey> builder = ImmutableSet.builder();
        JsonNode vlanPortPair = object.get(deviceId.toString());
        if (vlanPortPair != null) {
            vlanPortPair.forEach(jsonNode -> {
                if (!hasOnlyFields((ObjectNode) jsonNode, VLAN, PORTS, NAME)) {
                    throw new IllegalArgumentException(UNEXPECTED_FIELD_NAME);
                }
                VlanId vlanId = VlanId.vlanId((short) jsonNode.get(VLAN).asInt());
                builder.add(new XConnectStoreKey(deviceId, vlanId));
            });
        }
        return builder.build();
    }

    /**
     * Returns ports of given xconnect key.
     *
     * @param xconnect xconnect key
     * @return set of two ports associated with given xconnect key
     * @throws IllegalArgumentException if wrong format
     */
    public Set<PortNumber> getPorts(XConnectStoreKey xconnect) {
        ImmutableSet.Builder<PortNumber> builder = ImmutableSet.builder();
        object.get(xconnect.deviceId().toString()).forEach(vlanPortsPair -> {
            if (xconnect.vlanId().toShort() == vlanPortsPair.get(VLAN).asInt()) {
                int portCount = vlanPortsPair.get(PORTS).size();
                checkArgument(portCount == 2,
                        "Expect 2 ports but found " + portCount + " on " + xconnect);
                vlanPortsPair.get(PORTS).forEach(portNode -> {
                    builder.add(PortNumber.portNumber(portNode.asInt()));
                });
            }
        });
        return builder.build();
    }
}
