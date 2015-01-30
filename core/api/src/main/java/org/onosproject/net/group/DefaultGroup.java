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
package org.onosproject.net.group;

import static org.slf4j.LoggerFactory.getLogger;

import org.onosproject.core.GroupId;
import org.slf4j.Logger;

/**
 * ONOS implementation of default group that is stored in the system.
 */
public class DefaultGroup extends DefaultGroupDescription
    implements Group, StoredGroupEntry {

    private final Logger log = getLogger(getClass());

    private GroupState state;
    private long life;
    private long packets;
    private long bytes;
    private GroupId id;

    /**
     * Default group object constructor with the parameters.
     *
     * @param id group identifier
     * @param groupDesc group description parameters
     */
    public DefaultGroup(GroupId id, GroupDescription groupDesc) {
        super(groupDesc);
        this.id = id;
        this.state = GroupState.PENDING_ADD;
        this.life = 0;
        this.packets = 0;
        this.bytes = 0;
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

}
