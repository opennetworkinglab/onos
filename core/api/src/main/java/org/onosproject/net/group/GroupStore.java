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

import java.util.Collection;

import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.store.Store;

/**
 * Manages inventory of groups per device; not intended for direct use.
 */
public interface GroupStore extends Store<GroupEvent, GroupStoreDelegate> {

    enum UpdateType {
        /**
         * Modify existing group entry by adding provided information.
         */
        ADD,
        /**
         * Modify existing group by removing provided information from it.
         */
        REMOVE
    }

    /**
     * Returns the number of groups for the specified device in the store.
     *
     * @param deviceId the device ID
     * @return number of groups for the specified device
     */
    int getGroupCount(DeviceId deviceId);

    /**
     * Returns the groups associated with a device.
     *
     * @param deviceId the device ID
     * @return the group entries
     */
    Iterable<Group> getGroups(DeviceId deviceId);

    /**
     * Returns the stored group entry.
     *
     * @param deviceId the device ID
     * @param appCookie the group key
     * @return a group associated with the key
     */
    Group getGroup(DeviceId deviceId, GroupKey appCookie);

    /**
     * Returns the stored group entry for an id.
     *
     * @param deviceId the device ID
     * @param groupId the group identifier
     * @return a group associated with the key
     */
    Group getGroup(DeviceId deviceId, GroupId groupId);

    /**
     * Stores a new group entry using the information from group description.
     *
     * @param groupDesc group description to be used to store group entry
     */
    void storeGroupDescription(GroupDescription groupDesc);

    /**
     * Updates the existing group entry with the information
     * from group description.
     *
     * @param deviceId the device ID
     * @param oldAppCookie the current group key
     * @param type update type
     * @param newBuckets group buckets for updates
     * @param newAppCookie optional new group key
     */
    void updateGroupDescription(DeviceId deviceId,
                                GroupKey oldAppCookie,
                                UpdateType type,
                                GroupBuckets newBuckets,
                                GroupKey newAppCookie);

    /**
     * Triggers deleting the existing group entry.
     *
     * @param deviceId the device ID
     * @param appCookie the group key
     */
    void deleteGroupDescription(DeviceId deviceId,
                                GroupKey appCookie);

    /**
     * Stores a new group entry, or updates an existing entry.
     *
     * @param group group entry
     */
    void addOrUpdateGroupEntry(Group group);

    /**
     * Removes the group entry from store.
     *
     * @param group group entry
     */
    void removeGroupEntry(Group group);

    /**
     * Removes all group entries of given device from store.
     *
     * @param deviceId device id
     */
    void purgeGroupEntry(DeviceId deviceId);

    /**
     * Removes all group entries from store.
     */
    void purgeGroupEntries();

    /**
     * A group entry that is present in switch but not in the store.
     *
     * @param group group entry
     */
    void addOrUpdateExtraneousGroupEntry(Group group);

    /**
     * Remove the group entry from extraneous database.
     *
     * @param group group entry
     */
    void removeExtraneousGroupEntry(Group group);

    /**
     * Returns the extraneous groups associated with a device.
     *
     * @param deviceId the device ID
     *
     * @return the extraneous group entries
     */
    Iterable<Group> getExtraneousGroups(DeviceId deviceId);

    /**
     * Indicates the first group audit is completed.
     *
     * @param deviceId the device ID
     * @param completed initial audit status
     */
    void deviceInitialAuditCompleted(DeviceId deviceId, boolean completed);

    /**
     * Retrieves the initial group audit status for a device.
     *
     * @param deviceId the device ID
     *
     * @return initial group audit status
     */
    boolean deviceInitialAuditStatus(DeviceId deviceId);

    /**
     * Indicates the group operations failed.
     *
     * @param deviceId the device ID
     * @param operation the group operation failed
     */
    void groupOperationFailed(DeviceId deviceId, GroupOperation operation);

    /**
     * Submits the group metrics to store for a given device ID.
     *
     * @param deviceId the device ID
     * @param groupEntries the group entries as received from southbound
     */
    void pushGroupMetrics(DeviceId deviceId, Collection<Group> groupEntries);
}
