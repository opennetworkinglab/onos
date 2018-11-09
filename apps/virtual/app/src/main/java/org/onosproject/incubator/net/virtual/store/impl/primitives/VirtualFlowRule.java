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
import org.onosproject.net.flow.FlowRule;

import java.util.Objects;

/**
 * A wrapper class to encapsulate flow rule.
 */
public class VirtualFlowRule {
    NetworkId networkId;
    FlowRule rule;

    public VirtualFlowRule(NetworkId networkId, FlowRule rule) {
        this.networkId = networkId;
        this.rule = rule;
    }

    public NetworkId networkId() {
        return networkId;
    }

    public FlowRule rule() {
        return rule;
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId, rule);
    }

    @Override
    public boolean equals(Object other) {
        if (this ==  other) {
            return true;
        }

        if (other instanceof VirtualFlowRule) {
            VirtualFlowRule that = (VirtualFlowRule) other;
            return this.networkId.equals(that.networkId) &&
                    this.rule.equals(that.rule);
        } else {
            return false;
        }
    }
}


