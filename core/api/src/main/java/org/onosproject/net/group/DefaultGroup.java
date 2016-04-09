/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.group;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;

/**
 * ONOS implementation of default group that is stored in the system.
 */
public class DefaultGroup extends DefaultGroupDescription
    implements Group, StoredGroupEntry {

    private GroupState state;
    private boolean isGroupStateAddedFirstTime;
    private long life;
    private long packets;
    private long bytes;
    private long referenceCount;
    private GroupId id;
    private int age;

    /**
     * Initializes default values.
     *
     * @param newId group id for new group
     */
    private void initialize(GroupId newId) {
        id = newId;
        state = GroupState.PENDING_ADD;
        life = 0;
        packets = 0;
        bytes = 0;
        referenceCount = 0;
        age = 0;
    }

    /**
     * Default group object constructor with the parameters.
     *
     * @param id group identifier
     * @param groupDesc group description parameters
     */
    public DefaultGroup(GroupId id, GroupDescription groupDesc) {
        super(groupDesc);
        initialize(id);
    }

    /**
     * Default group object constructor with the available information
     * from data plane.
     *
     * @param id group identifier
     * @param deviceId device identifier
     * @param type type of the group
     * @param buckets immutable list of group bucket
     */
    public DefaultGroup(GroupId id,
                        DeviceId deviceId,
                        GroupDescription.Type type,
                        GroupBuckets buckets) {
        super(deviceId, type, buckets);
        initialize(id);
    }

    /**
     * Returns group identifier associated with a group object.
     *
     * @return GroupId Group Identifier
     */
    @Override
    public GroupId id() {
        return this.id;
    }

    /**
     * Returns current state of a group object.
     *
     * @return GroupState Group State
     */
    @Override
    public GroupState state() {
        return this.state;
    }

    /**
     * Returns the number of milliseconds this group has been alive.
     *
     * @return number of millis
     */
    @Override
    public long life() {
        return this.life;
    }

    /**
     * Returns the number of packets processed by this group.
     *
     * @return number of packets
     */
    @Override
    public long packets() {
        return this.packets;
    }

    /**
     * Returns the number of bytes processed by this group.
     *
     * @return number of bytes
     */
    @Override
    public long bytes() {
        return this.bytes;
    }

    @Override
    public int age() {
        return age;
    }

    /**
     * Sets the new state for this entry.
     *
     * @param newState new group entry state.
     */
    @Override
    public void setState(Group.GroupState newState) {
        this.state = newState;
    }

    /**
     * Sets how long this entry has been entered in the system.
     *
     * @param life epoch time
     */
    @Override
    public void setLife(long life) {
        this.life = life;
    }

    /**
     * Sets number of packets processed by this group entry.
     *
     * @param packets a long value
     */
    @Override
    public void setPackets(long packets) {
        this.packets = packets;
    }

    /**
     * Sets number of bytes processed by this group entry.
     *
     * @param bytes a long value
     */
    @Override
    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    @Override
    public void setReferenceCount(long referenceCount) {
        this.referenceCount = referenceCount;
        if (referenceCount == 0) {
            age++;
        } else {
            age = 0;
        }
    }

    @Override
    public long referenceCount() {
        return referenceCount;
    }

    /*
     * The deviceId, type and buckets are used for hash.
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }

    /*
     * The deviceId, groupId, type and buckets should be same.
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
       if (obj instanceof DefaultGroup) {
            DefaultGroup that = (DefaultGroup) obj;
            return super.equals(obj) &&
                    Objects.equals(id, that.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("description", super.toString())
                .add("groupid", id)
                .add("state", state)
                .add("age", age)
                .toString();
    }

    @Override
    public void setIsGroupStateAddedFirstTime(boolean isGroupStateAddedFirstTime) {
        this.isGroupStateAddedFirstTime = isGroupStateAddedFirstTime;
    }

    @Override
    public boolean isGroupStateAddedFirstTime() {
        return isGroupStateAddedFirstTime;
    }
}
