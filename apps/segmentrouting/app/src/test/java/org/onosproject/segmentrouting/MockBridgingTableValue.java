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

import org.onosproject.net.PortNumber;

import java.util.Objects;

/**
 * Mock Bridging Table Value.
 */
class MockBridgingTableValue {
    boolean popVlan;
    PortNumber portNumber;

    MockBridgingTableValue(boolean popVlan, PortNumber portNumber) {
        this.popVlan = popVlan;
        this.portNumber = portNumber;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MockBridgingTableValue)) {
            return false;
        }
        final MockBridgingTableValue other = (MockBridgingTableValue) obj;
        return Objects.equals(this.popVlan, other.popVlan) &&
                Objects.equals(this.portNumber, other.portNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(popVlan, portNumber);
    }
}