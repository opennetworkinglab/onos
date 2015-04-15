/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.net.flow;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

public class DefaultFlowRule implements FlowRule {

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

    private final Type type;


    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
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
        this.type = Type.DEFAULT;
    }

    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
                           TrafficTreatment treatment, int priority, long flowId,
                           int timeout, boolean permanent, Type tableType) {
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
        this.type = tableType;
    }

    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
                           TrafficTreatment treatment, int priority, ApplicationId appId,
                           int timeout, boolean permanent) {
        this(deviceId, selector, treatment, priority, appId, new DefaultGroupId(0),
                timeout, permanent);
    }

    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
                           TrafficTreatment treatment, int priority, ApplicationId appId,
                           int timeout, boolean permanent, Type type) {

        if (priority < FlowRule.MIN_PRIORITY) {
            throw new IllegalArgumentException("Priority cannot be less than " + MIN_PRIORITY);
        }

        this.deviceId = deviceId;
        this.priority = priority;
        this.selector = selector;
        this.treatment = treatment;
        this.appId = appId.id();
        this.groupId = new DefaultGroupId(0);
        this.timeout = timeout;
        this.permanent = permanent;
        this.created = System.currentTimeMillis();
        this.type = type;

        /*
         * id consists of the following.
         * | appId (16 bits) | groupId (16 bits) | flowId (32 bits) |
         */
        this.id = FlowId.valueOf((((long) this.appId) << 48) | (((long) this.groupId.id()) << 32)
                | (this.hash() & 0xffffffffL));

    }

    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
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
        this.type = Type.DEFAULT;

        /*
         * id consists of the following.
         * | appId (16 bits) | groupId (16 bits) | flowId (32 bits) |
         */
        this.id = FlowId.valueOf((((long) this.appId) << 48) | (((long) this.groupId.id()) << 32)
                | (this.hash() & 0xffffffffL));
    }

    public DefaultFlowRule(FlowRule rule) {
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
        this.type = rule.type();

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
    /*
     * The priority and statistics can change on a given treatment and selector
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public int hashCode() {
        return Objects.hash(deviceId, selector, priority, type);
    }

    public int hash() {
        return Objects.hash(deviceId, selector, treatment, type);
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
        if (obj instanceof DefaultFlowRule) {
            DefaultFlowRule that = (DefaultFlowRule) obj;
            return Objects.equals(deviceId, that.deviceId) &&
                    Objects.equals(priority, that.priority) &&
                    Objects.equals(selector, that.selector) &&
                    Objects.equals(type, that.type);

        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", Long.toHexString(id.value()))
                .add("deviceId", deviceId)
                .add("priority", priority)
                .add("selector", selector.criteria())
                .add("treatment", treatment == null ? "N/A" : treatment.allInstructions())
                .add("table type", type)
                .add("created", created)
                .toString();
    }

    @Override
    public int timeout() {
        return timeout;
    }

    @Override
    public boolean isPermanent() {
        return permanent;
    }

    @Override
    public Type type() {
        return type;
    }

}
