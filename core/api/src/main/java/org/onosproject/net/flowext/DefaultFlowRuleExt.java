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
package org.onosproject.net.flowext;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Experimental extension to the flow rule subsystem; still under development.
 * A temporary flow rule extend implementation, It will cover current onos flow rule and other flow extension.
 */
public class DefaultFlowRuleExt
        extends DefaultFlowRule implements FlowRuleExt {

    private FlowEntryExtension flowEntryExtension;

    public DefaultFlowRuleExt(DeviceId deviceId, TrafficSelector selector,
                              TrafficTreatment treatment, int priority, long flowId,
                              int timeout, boolean permanent) {
        super(deviceId, selector, treatment, priority, flowId, timeout, permanent);
    }

    public DefaultFlowRuleExt(DeviceId deviceId, TrafficSelector selector,
                              TrafficTreatment treatment, int priority, ApplicationId appId,
                              int timeout, boolean permanent) {
        this(deviceId, selector, treatment, priority, appId, new DefaultGroupId(0),
             timeout, permanent);
    }

    public DefaultFlowRuleExt(DeviceId deviceId, TrafficSelector selector,
                              TrafficTreatment treatment, int priority, ApplicationId appId,
                              GroupId groupId, int timeout, boolean permanent) {
        super(deviceId, selector, treatment, priority, appId, groupId, timeout, permanent);
    }

    public DefaultFlowRuleExt(FlowRule rule) {
        super(rule);
    }

    public DefaultFlowRuleExt(ApplicationId appId, DeviceId deviceId, FlowEntryExtension data) {
        this(deviceId, null, null, FlowRule.MIN_PRIORITY, appId, 0, false);
        this.flowEntryExtension = data;
    }

    @Override
    public FlowEntryExtension getFlowEntryExt() {
        return this.flowEntryExtension;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(flowEntryExtension);
    }

    public int hash() {
        return 31 * super.hashCode() + Objects.hash(flowEntryExtension);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final DefaultFlowRuleExt other = (DefaultFlowRuleExt) obj;
        return Objects.equals(this.flowEntryExtension, other.flowEntryExtension);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                // TODO there might be a better way to grab super's string
                .add("id", Long.toHexString(id().value()))
                .add("deviceId", deviceId())
                .add("priority", priority())
                .add("selector", selector().criteria())
                .add("treatment", treatment() == null ? "N/A" : treatment().instructions())
                        //.add("created", created)
                .add("flowEntryExtension", flowEntryExtension)
                .toString();
    }
}
