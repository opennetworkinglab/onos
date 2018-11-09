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

package org.onosproject.incubator.net.virtual;

import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupStoreDelegate;

import java.util.Collection;

/**
 * Manages inventory of groups per virtual network and virtual device;
 * not intended for direct use.
 */
public interface VirtualNetworkGroupStore
        extends VirtualStore<GroupEvent, GroupStoreDelegate> {

    enum UpdateType {
        /**
         * Modify existing group entry by adding provided information.
         */
        ADD,
        /**
         * Modify existing group by removing provided information from it.
         */
        REMOVE,
        /**
         * Modify existing group entry by setting the provided information,
         * overwriting the previous group entry entirely.
         */
        SET
    }

    /**
     * Returns the number of groups for the specified virtual device in the store.
     *
     * @param networkId the virtual network ID
     * @param deviceId the device ID
     * @return number of groups for the specified device
     */
    int getGroupCount(NetworkId networkId, DeviceId deviceId);

    /**
     * Returns the groups associated with a virtual device.
     *
     * @param networkId the virtual network ID
     * @param deviceId the device ID
     * @return the group entries
     */
    Iterable<Group> getGroups(NetworkId networkId, DeviceId deviceId);

    /**
     * Returns the stored group entry in a virtual network.
     *
     * @param networkId the virtual network ID
     * @param deviceId the device ID
     * @param appCookie the group key
     * @return a group associated with the key
     */
    Group getGroup(NetworkId networkId, DeviceId deviceId, GroupKey appCookie);

    /**
     * Returns the stored group entry for an id.
     *
     * @param networkId the virtual network ID
     * @param deviceId the device ID
     * @param groupId the group identifier
     * @return a group associated with the key
     */
    Group getGroup(NetworkId networkId, DeviceId deviceId, GroupId groupId);

    /**
     * Stores a new group entry using the information from group description
     * for a virtual network.
     *
     * @param networkId the virtual network ID
     * @param groupDesc group description to be used to store group entry
     */
    void storeGroupDescription(NetworkId networkId, GroupDescription groupDesc);

    /**
     * Updates the existing group entry with the information
     * from group description.
     *
     * @param networkId the virtual network ID
     * @param deviceId the device ID
     * @param oldAppCookie the current group key
     * @param type update type
     * @param newBuckets group buckets for updates
     * @param newAppCookie optional new group key
     */
    void updateGroupDescription(NetworkId networkId,
                                DeviceId deviceId,
                                GroupKey oldAppCookie,
                                UpdateType type,
                                GroupBuckets newBuckets,
                                GroupKey newAppCookie);

    /**
     * Triggers deleting the existing group entry.
     *
     * @param networkId the virtual network ID
     * @param deviceId the device ID
     * @param appCookie the group key
     */
    void deleteGroupDescription(NetworkId networkId,
                                DeviceId deviceId,
                                GroupKey appCookie);

    /**
     * Stores a new group entry, or updates an existing entry.
     *
     * @param networkId the virtual network ID
     * @param group group entry
     */
    void addOrUpdateGroupEntry(NetworkId networkId, Group group);

    /**
     * Removes the group entry from store.
     *
     * @param networkId the virtual network ID
     * @param group group entry
     */
    void removeGroupEntry(NetworkId networkId, Group group);

    /**
     * Removes all group entries of given device from store.
     *
     * @param networkId the virtual network ID
     * @param deviceId device id
     */
    void purgeGroupEntry(NetworkId networkId, DeviceId deviceId);

    /**
     * Removes all group entries from store.
     *
     * @param networkId the virtual network ID
     */
    default void purgeGroupEntries(NetworkId networkId) {}

    /**
     * A group entry that is present in switch but not in the store.
     *
     * @param networkId the virtual network ID
     * @param group group entry
     */
    void addOrUpdateExtraneousGroupEntry(NetworkId networkId, Group group);

    /**
     * Remove the group entry from extraneous database.
     *
     * @param networkId the virtual network ID
     * @param group group entry
     */
    void removeExtraneousGroupEntry(NetworkId networkId, Group group);

    /**
     * Returns the extraneous groups associated with a device.
     *
     * @param networkId the virtual network ID
     * @param deviceId the device ID
     *
     * @return the extraneous group entries
     */
    Iterable<Group> getExtraneousGroups(NetworkId networkId, DeviceId deviceId);

    /**
     * Indicates the first group audit is completed.
     *
     * @param networkId the virtual network ID
     * @param deviceId the device ID
     * @param completed initial audit status
     */
    void deviceInitialAuditCompleted(NetworkId networkId, DeviceId deviceId, boolean completed);

    /**
     * Retrieves the initial group audit status for a device.
     *
     * @param networkId the virtual network ID
     * @param deviceId the device ID
     *
     * @return initial group audit status
     */
    boolean deviceInitialAuditStatus(NetworkId networkId, DeviceId deviceId);

    /**
     * Indicates the group operations failed.
     *
     * @param networkId the virtual network ID
     * @param deviceId the device ID
     * @param operation the group operation failed
     */
    void groupOperationFailed(NetworkId networkId, DeviceId deviceId, GroupOperation operation);

    /**
     * Submits the group metrics to store for a given device ID.
     *
     * @param networkId the virtual network ID
     * @param deviceId the device ID
     * @param groupEntries the group entries as received from southbound
     */
    void pushGroupMetrics(NetworkId networkId, DeviceId deviceId, Collection<Group> groupEntries);

    /**
     * Indicates failover within a failover group.
     *
     * @param networkId the virtual network ID
     * @param failoverGroups groups to notify
     */
    void notifyOfFailovers(NetworkId networkId, Collection<Group> failoverGroups);
}
