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

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.PortNumber;

import java.util.Objects;

/**
 * Mock Routing Table Value.
 */
class MockRoutingTableValue {
    PortNumber portNumber;
    MacAddress macAddress;
    VlanId vlanId;

    MockRoutingTableValue(PortNumber portNumber, MacAddress macAddress, VlanId vlanId) {
        this.portNumber = portNumber;
        this.macAddress = macAddress;
        this.vlanId = vlanId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MockRoutingTableValue)) {
            return false;
        }
        final MockRoutingTableValue other = (MockRoutingTableValue) obj;
        return Objects.equals(this.portNumber, other.portNumber) &&
                Objects.equals(this.macAddress, other.macAddress) &&
                Objects.equals(this.vlanId, other.vlanId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portNumber, macAddress, vlanId);
    }
}

