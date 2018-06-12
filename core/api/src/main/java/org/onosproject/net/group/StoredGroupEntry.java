/*
 * Copyright 2015-present Open Networking Foundation
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

/**
 * Interface that defines set methods for a group entry
 * that is stored in the system.
 */
public interface StoredGroupEntry extends Group {

    /**
     * Sets the new state for this entry.
     *
     * @param newState new group entry state.
     */
    void setState(Group.GroupState newState);

    /**
     * Sets if group has transitioned to ADDED state for the first time.
     * This is to differentiate state transitions "from PENDING_ADD to ADDED"
     * and "from PENDING_UPDATE to ADDED". For internal use only.
     *
     * @param isGroupAddedFirstTime true if group moves to ADDED state
     * for the first time.
     */
    void setIsGroupStateAddedFirstTime(boolean isGroupAddedFirstTime);

    /**
     * Returns the isGroupStateAddedFirstTime value. For internal use only.
     *
     * @return isGroupStateAddedFirstTime value
     */
    boolean isGroupStateAddedFirstTime();

    /**
     * Sets how long this entry has been entered in the system.
     *
     * @param life epoch time
     */
    void setLife(long life);

    /**
     * Sets number of packets processed by this group entry.
     *
     * @param packets a long value
     */
    void setPackets(long packets);

    /**
     * Sets number of bytes processed by this group entry.
     *
     * @param bytes a long value
     */
    void setBytes(long bytes);

    /**
     * Sets number of flow rules or groups referencing this group entry.
     *
     * @param referenceCount reference count
     */
    void setReferenceCount(long referenceCount);

    /**
     * Increments the count for the number of failed attempts in programming
     * this group.
     *
     */
    void incrFailedRetryCount();

    /**
     * Sets the count for the number of failed attempts in programming this
     * group.
     *
     * @param failedRetryCount count for number of failed attempts in
     *            programming this group
     */
    void setFailedRetryCount(int failedRetryCount);
}
