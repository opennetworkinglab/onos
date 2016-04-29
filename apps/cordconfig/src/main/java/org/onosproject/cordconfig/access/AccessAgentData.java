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
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Information about an access agent.
 */
public class AccessAgentData {
    private static final String DEVICE_ID_MISSING = "Device ID cannot be null";
    private static final String OLT_INFO_MISSING = "OLT information cannot be null";
    private static final String AGENT_MAC_MISSING = "Agent mac cannot be null";
    private static final String VTN_MISSING = "VTN location cannot be null";


    private final Map<ConnectPoint, MacAddress> oltMacInfo;
    private final MacAddress agentMac;
    private final Optional<ConnectPoint> vtnLocation;
    private final DeviceId deviceId;


    /**
     * Constucts an agent configuration for a given device.
     *
     * @param deviceId    access device id
     * @param oltMacInfo  a map of olt chips and their mac address
     * @param agentMac    the mac address of the agent
     * @param vtnLocation the location of the agent
     */
    public AccessAgentData(DeviceId deviceId, Map<ConnectPoint, MacAddress> oltMacInfo,
                           MacAddress agentMac, Optional<ConnectPoint> vtnLocation) {
        this.deviceId = checkNotNull(deviceId, DEVICE_ID_MISSING);
        this.oltMacInfo = checkNotNull(oltMacInfo, OLT_INFO_MISSING);
        this.agentMac = checkNotNull(agentMac, AGENT_MAC_MISSING);
        this.vtnLocation = checkNotNull(vtnLocation, VTN_MISSING);
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
     * Returns the mapping of olt chips to mac addresses. Each chip is
     * symbolized by a connect point.
     *
     * @return a mapping of chips (as connect points) to mac addresses
     */
    public Map<ConnectPoint, MacAddress> getOltMacInfo() {
        return ImmutableMap.copyOf(oltMacInfo);
    }

    /**
     * Reuturns the agents mac address.
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
}
