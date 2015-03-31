/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.packet;

import com.google.common.base.MoreObjects;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;

/**
 * Default implementation of a packet request.
 */
public final class DefaultPacketRequest implements PacketRequest {
    private final TrafficSelector selector;
    private final PacketPriority priority;
    private final ApplicationId appId;
    private final FlowRule.Type tableType;

    public DefaultPacketRequest(TrafficSelector selector, PacketPriority priority,
                                ApplicationId appId, FlowRule.Type tableType) {
        this.selector = selector;
        this.priority = priority;
        this.appId = appId;
        this.tableType = tableType;
    }

    public TrafficSelector selector() {
        return selector;
    }

    public PacketPriority priority() {
        return priority;
    }

    public ApplicationId appId() {
        return appId;
    }

    public FlowRule.Type tableType() {
        return tableType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultPacketRequest that = (DefaultPacketRequest) o;

        if (priority != that.priority) {
            return false;
        }
        if (!selector.equals(that.selector)) {
            return false;
        }
        if (!tableType.equals(that.tableType)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = selector.hashCode();
        result = 31 * result + priority.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass())
                .add("selector", selector)
                .add("priority", priority)
                .add("appId", appId)
                .add("table-type", tableType).toString();
    }
}