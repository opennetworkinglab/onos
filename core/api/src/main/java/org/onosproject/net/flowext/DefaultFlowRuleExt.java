/*
 * Copyright 2014 Open Networking Laboratory
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

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

/**
 * A temporary flow rule extend implementation, It will cover current onos flow rule and other flow extension.
 * Its still being provisional / experimental at this point in time.
 */
public class DefaultFlowRuleExt implements FlowRuleExt {
    private final DeviceId deviceId;
    private final int priority;
    private final TrafficSelector selector;
    private final TrafficTreatment treatment;
    private final long created;
    private final FlowId id;
    private final short appId;
    private final int timeout;
    private final boolean permanent;
    private final GroupId groupId;
    private FlowEntryExtension flowEntryExtension;
    public DefaultFlowRuleExt(DeviceId deviceId, TrafficSelector selector,
                           TrafficTreatment treatment, int priority, long flowId,
                           int timeout, boolean permanent) {
        this.deviceId = deviceId;
        this.priority = priority;
        this.selector = selector;
        this.treatment = treatment;
        this.timeout = timeout;
        this.permanent = permanent;
        this.created = System.currentTimeMillis();

        this.appId = (short) (flowId >>> 48);
        this.groupId = new DefaultGroupId((short) ((flowId >>> 32) & 0xFFFF));
        this.id = FlowId.valueOf(flowId);
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

        if (priority < FlowRule.MIN_PRIORITY) {
            throw new IllegalArgumentException("Priority cannot be less than " + MIN_PRIORITY);
        }

        this.deviceId = deviceId;
        this.priority = priority;
        this.selector = selector;
        this.treatment = treatment;
        this.appId = appId.id();
        this.groupId = groupId;
        this.timeout = timeout;
        this.permanent = permanent;
        this.created = System.currentTimeMillis();

        /*
         * id consists of the following.
         * | appId (16 bits) | groupId (16 bits) | flowId (32 bits) |
         */
        this.id = FlowId.valueOf((((long) this.appId) << 48) | (((long) this.groupId.id()) << 32)
                | (this.hash() & 0xffffffffL));
    }

    public DefaultFlowRuleExt(FlowRule rule) {
        this.deviceId = rule.deviceId();
        this.priority = rule.priority();
        this.selector = rule.selector();
        this.treatment = rule.treatment();
        this.appId = rule.appId();
        this.groupId = rule.groupId();
        this.id = rule.id();
        this.timeout = rule.timeout();
        this.permanent = rule.isPermanent();
        this.created = System.currentTimeMillis();
    }

    public DefaultFlowRuleExt(ApplicationId appId, DeviceId deviceId, FlowEntryExtension data) {
        this.deviceId = deviceId;
        this.flowEntryExtension = data;
        this.appId = appId.id();
        this.priority = FlowRule.MIN_PRIORITY;
        this.selector = null;
        this.treatment = null;
        this.timeout = 0;
        this.permanent = false;
        this.created = System.currentTimeMillis();
        this.groupId = new DefaultGroupId(0);
        /*
         * id consists of the following.
         * | appId (16 bits) | groupId (16 bits) | flowId (32 bits) |
         */
        this.id = FlowId.valueOf((((long) this.appId) << 48) | (((long) this.groupId.id()) << 32)
                | (this.hash() & 0xffffffffL));
    }

    @Override
    public FlowId id() {
        return id;
    }

    @Override
    public short appId() {
        return appId;
    }

    @Override
    public GroupId groupId() {
        return groupId;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public TrafficSelector selector() {
        return selector;
    }

    @Override
    public TrafficTreatment treatment() {
        return treatment;
    }
    @Override
    public int timeout() {
        return timeout;
    }

    @Override
    public boolean isPermanent() {
        return permanent;
    }

    public FlowEntryExtension getFlowEntryExt() {
        return this.flowEntryExtension;
    }

    @Override
    /*
     * The priority and statistics can change on a given treatment and selector
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public int hashCode() {
        return Objects.hash(deviceId, selector, priority, flowEntryExtension);
    }

    public int hash() {
        return Objects.hash(deviceId, selector, treatment, flowEntryExtension);
    }

    @Override
    /*
     * The priority and statistics can change on a given treatment and selector
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultFlowRuleExt) {
            DefaultFlowRuleExt that = (DefaultFlowRuleExt) obj;
            return Objects.equals(deviceId, that.deviceId) &&
                    Objects.equals(priority, that.priority) &&
                    Objects.equals(selector, that.selector) &&
                    Objects.equals(flowEntryExtension, that.flowEntryExtension);

        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", Long.toHexString(id.value()))
                .add("deviceId", deviceId)
                .add("priority", priority)
                .add("selector", selector == null ? "N/A" : selector.criteria())
                .add("treatment", treatment == null ? "N/A" : treatment.instructions())
                .add("created", created)
                .toString();
    }
}
