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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Information about an access agent.
 */
public class AccessAgentData {

    private static final String DEVICE_ID_MISSING = "Device ID cannot be null";
    private static final String OLT_INFO_MISSING = "OLT information cannot be null";
    private static final String AGENT_MAC_MISSING = "Agent mac cannot be null";
    private static final String VTN_MISSING = "VTN location cannot be null";

    private static final int CHIP_PORT_RANGE_SIZE = 130;

    private final Map<ConnectPoint, MacAddress> oltMacInfo;
    private final MacAddress agentMac;
    private final Optional<ConnectPoint> vtnLocation;
    private final DeviceId deviceId;

    // OLT chip information sorted by ascending MAC address
    private final List<Pair<ConnectPoint, MacAddress>> sortedOltChips;

    /**
     * Constructs an agent configuration for a given device.
     *
     * @param deviceId    access device ID
     * @param oltMacInfo  a map of olt chips and their mac address
     * @param agentMac    the MAC address of the agent
     * @param vtnLocation the location of the agent
     */
    public AccessAgentData(DeviceId deviceId, Map<ConnectPoint, MacAddress> oltMacInfo,
                           MacAddress agentMac, Optional<ConnectPoint> vtnLocation) {
        this.deviceId = checkNotNull(deviceId, DEVICE_ID_MISSING);
        this.oltMacInfo = ImmutableMap.copyOf(checkNotNull(oltMacInfo, OLT_INFO_MISSING));
        this.agentMac = checkNotNull(agentMac, AGENT_MAC_MISSING);
        this.vtnLocation = checkNotNull(vtnLocation, VTN_MISSING);

        this.sortedOltChips = oltMacInfo.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e1.getValue().toLong(), e2.getValue().toLong()))
                .map(e -> Pair.of(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the access device ID.
     *
     * @return device ID
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Returns the mapping of OLT chips to MAC addresses. Each chip is
     * symbolized by a connect point.
     *
     * @return a mapping of chips (as connect points) to MAC addresses
     */
    public Map<ConnectPoint, MacAddress> getOltMacInfo() {
        return oltMacInfo;
    }

    /**
     * Returns the agent's MAC address.
     *
     * @return a mac address
     */
    public MacAddress getAgentMac() {
        return agentMac;
    }

    /**
     * Returns the location of the agent.
     *
     * @return a connection point
     */
    public Optional<ConnectPoint> getVtnLocation() {
        return vtnLocation;
    }

    /**
     * Returns the point where the OLT is connected to the fabric given a
     * connect point on the agent device.
     *
     * @param agentConnectPoint connect point on the agent device
     * @return point were OLT is connected to fabric
     */
    public Optional<ConnectPoint> getOltConnectPoint(ConnectPoint agentConnectPoint) {
        int index = ((int) agentConnectPoint.port().toLong()) / CHIP_PORT_RANGE_SIZE;

        if (index >= sortedOltChips.size()) {
            return Optional.empty();
        }

        return Optional.of(sortedOltChips.get(index).getKey());
    }
}
