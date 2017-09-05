/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.ofagent.api;

import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.net.group.Group;

import java.util.List;
import java.util.Set;

/**
 * Service for providing virtual OpenFlow switch information.
 */
public interface OFSwitchService {

    /**
     * Returns all openflow switches that OF agent service manages.
     *
     * @return set of openflow switches; empty set if no openflow switches exist
     */
    Set<OFSwitch> ofSwitches();

    /**
     * Returns all openflow switches for the specified network.
     *
     * @param networkId network id
     * @return set of openflow switches; empty set if no devices exist on the network
     */
    Set<OFSwitch> ofSwitches(NetworkId networkId);

    /**
     * Returns openflow switch for the specified device in the specified network.
     *
     * @param networkId network id
     * @param deviceId device id
     * @return openflow switch; null if none exists
     */
    OFSwitch ofSwitch(NetworkId networkId, DeviceId deviceId);

    /**
     * Returns all ports of the specified device in the specified network.
     *
     * @param networkId network id
     * @param deviceId device id
     * @return set of ports; empty set if no ports exist for the specified device
     */
    Set<Port> ports(NetworkId networkId, DeviceId deviceId);

    /**
     * Returns all port statistics of the specified device in the specified network.
     *
     * @param networkId network id
     * @param deviceId device id
     * @return list of port statistics; empty list if none exists for the specified device
     */
    List<PortStatistics> getPortStatistics(NetworkId networkId, DeviceId deviceId);

    /**
     * Returns all flow entries of the specified device in the specified network.
     *
     * @param networkId network id
     * @param deviceId device id
     * @return list of flow entries; empty list if none exists for the specified device
     */
    List<FlowEntry> getFlowEntries(NetworkId networkId, DeviceId deviceId);

    /**
     * Returns all flow table statistics of the specified device in the specified network.
     *
     * @param networkId network id
     * @param deviceId device id
     * @return list of flow table statistics; empty list if none exists for the specified device
     */
    List<TableStatisticsEntry> getFlowTableStatistics(NetworkId networkId, DeviceId deviceId);

    /**
     * Returns all groups associated with the specified device in the specified network.
     *
     * @param networkId network id
     * @param deviceId device id
     * @return list of  groups; empty list if none exists for the specified device
     */
    List<Group> getGroups(NetworkId networkId, DeviceId deviceId);

    /**
     * Returns neighbour port of the specified port in the specified network.
     *
     * @param networkId network id
     * @param deviceId device id
     * @param portNumber port number
     * @return connect point; null if none exists
     */
    ConnectPoint neighbour(NetworkId networkId, DeviceId deviceId, PortNumber portNumber);

    /**
     * Returns application id.
     *
     * @return application id
     */
    ApplicationId appId();
}
