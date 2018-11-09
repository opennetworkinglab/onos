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

package org.onosproject.incubator.net.virtual.store.impl.primitives;

import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.flow.FlowEntry;

import java.util.Objects;

/**
 * A wrapper class to encapsulate flow entry.
 */
public class VirtualFlowEntry {
    NetworkId networkId;
    FlowEntry flowEntry;

    public VirtualFlowEntry(NetworkId networkId, FlowEntry flowEntry) {
        this.networkId = networkId;
        this.flowEntry = flowEntry;
    }

    public NetworkId networkId() {
        return networkId;
    }

    public FlowEntry flowEntry() {
        return flowEntry;
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId, flowEntry);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other instanceof VirtualFlowEntry) {
            VirtualFlowEntry that = (VirtualFlowEntry) other;
            return this.networkId.equals(that.networkId) &&
                    this.flowEntry.equals(that.flowEntry);
        } else {
            return false;
        }
    }
}
