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

package org.onosproject.segmentrouting;

import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Map;

/**
 * Mock Routing Rule Populator.
 */
public class MockRoutingRulePopulator extends RoutingRulePopulator {
    private Map<MockRoutingTableKey, MockRoutingTableValue> routingTable;

    MockRoutingRulePopulator(SegmentRoutingManager srManager,
                             Map<MockRoutingTableKey, MockRoutingTableValue> routingTable) {
        super(srManager);
        this.routingTable = routingTable;
    }

    @Override
    public void populateRoute(DeviceId deviceId, IpPrefix prefix,
                              MacAddress hostMac, VlanId hostVlanId, PortNumber outPort) {
        MockRoutingTableKey rtKey = new MockRoutingTableKey(deviceId, prefix);
        MockRoutingTableValue rtValue = new MockRoutingTableValue(outPort, hostMac, hostVlanId);
        routingTable.put(rtKey, rtValue);
    }

    @Override
    public void revokeRoute(DeviceId deviceId, IpPrefix prefix,
                            MacAddress hostMac, VlanId hostVlanId, PortNumber outPort) {
        MockRoutingTableKey rtKey = new MockRoutingTableKey(deviceId, prefix);
        MockRoutingTableValue rtValue = new MockRoutingTableValue(outPort, hostMac, hostVlanId);
        routingTable.remove(rtKey, rtValue);
    }
}