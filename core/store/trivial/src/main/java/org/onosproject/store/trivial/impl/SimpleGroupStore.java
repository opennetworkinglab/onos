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
package org.onosproject.store.trivial.impl;

import static org.apache.commons.lang3.concurrent.ConcurrentUtils.createIfAbsentUnchecked;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.NewConcurrentHashMap;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.Group.GroupState;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupEvent.Type;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupStore;
import org.onosproject.net.group.GroupStoreDelegate;
import org.onosproject.net.group.StoredGroupEntry;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

/**
 * Manages inventory of group entries using trivial in-memory implementation.
 */
@Component(immediate = true)
@Service
public class SimpleGroupStore
        extends AbstractStore<GroupEvent, GroupStoreDelegate>
        implements GroupStore {

    private final Logger log = getLogger(getClass());

    private final int dummyId = 0xffffffff;
    private final GroupId dummyGroupId = new DefaultGroupId(dummyId);

    // inner Map is per device group table
    private final ConcurrentMap<DeviceId, ConcurrentMap<GroupKey, StoredGroupEntry>>
            groupEntriesByKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, ConcurrentMap<GroupId, StoredGroupEntry>>
            groupEntriesById = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, ConcurrentMap<GroupKey, StoredGroupEntry>>
            pendingGroupEntriesByKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, ConcurrentMap<GroupId, Group>>
        extraneousGroupEntriesById = new ConcurrentHashMap<>();

    private final HashMap<DeviceId, Boolean> deviceAuditStatus =
            new HashMap<DeviceId, Boolean>();

    private final AtomicInteger groupIdGen = new AtomicInteger();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        groupEntriesByKey.clear();
        groupEntriesById.clear();
        log.info("Stopped");
    }

    private static NewConcurrentHashMap<GroupKey, StoredGroupEntry>
                        lazyEmptyGroupKeyTable() {
        return NewConcurrentHashMap.<GroupKey, StoredGroupEntry>ifNeeded();
    }

    private static NewConcurrentHashMap<GroupId, StoredGroupEntry>
                        lazyEmptyGroupIdTable() {
        return NewConcurrentHashMap.<GroupId, StoredGroupEntry>ifNeeded();
    }

    private static NewConcurrentHashMap<GroupKey, StoredGroupEntry>
                        lazyEmptyPendingGroupKeyTable() {
        return NewConcurrentHashMap.<GroupKey, StoredGroupEntry>ifNeeded();
    }

    private static NewConcurrentHashMap<GroupId, Group>
                        lazyEmptyExtraneousGroupIdTable() {
        return NewConcurrentHashMap.<GroupId, Group>ifNeeded();
    }

    /**
     * Returns the group key table for specified device.
     *
     * @param deviceId identifier of the device
     * @return Map representing group key table of given device.
     */
    private ConcurrentMap<GroupKey, StoredGroupEntry> getGroupKeyTable(DeviceId deviceId) {
        return createIfAbsentUnchecked(groupEntriesByKey,
                                       deviceId, lazyEmptyGroupKeyTable());
    }

    /**
     * Returns the group id table for specified device.
     *
     * @param deviceId identifier of the device
     * @return Map representing group key table of given device.
     */
    private ConcurrentMap<GroupId, StoredGroupEntry> getGroupIdTable(DeviceId deviceId) {
        return createIfAbsentUnchecked(groupEntriesById,
                                       deviceId, lazyEmptyGroupIdTable());
    }

    /**
     * Returns the pending group key table for specified device.
     *
     * @param deviceId identifier of the device
     * @return Map representing group key table of given device.
     */
    private ConcurrentMap<GroupKey, StoredGroupEntry>
                    getPendingGroupKeyTable(DeviceId deviceId) {
        return createIfAbsentUnchecked(pendingGroupEntriesByKey,
                                       deviceId, lazyEmptyPendingGroupKeyTable());
    }

    /**
     * Returns the extraneous group id table for specified device.
     *
     * @param deviceId identifier of the device
     * @return Map representing group key table of given device.
     */
    private ConcurrentMap<GroupId, Group>
                getExtraneousGroupIdTable(DeviceId deviceId) {
        return createIfAbsentUnchecked(extraneousGroupEntriesById,
                                       deviceId,
                                       lazyEmptyExtraneousGroupIdTable());
    }

    /**
     * Returns the number of groups for the specified device in the store.
     *
     * @return number of groups for the specified device
     */
    @Override
    public int getGroupCount(DeviceId deviceId) {
        return (groupEntriesByKey.get(deviceId) != null) ?
                 groupEntriesByKey.get(deviceId).size() : 0;
    }

    /**
     * Returns the groups associated with a device.
     *
     * @param deviceId the device ID
     *
     * @return the group entries
     */
    @Override
    public Iterable<Group> getGroups(DeviceId deviceId) {
        // flatten and make iterator unmodifiable
        return FluentIterable.from(getGroupKeyTable(deviceId).values())
            .transform(
                    new Function<StoredGroupEntry, Group>() {

                        @Override
                        public Group apply(
                                StoredGroupEntry input) {
                            return input;
                        }
                    });
    }

    /**
     * Returns the stored group entry.
     *
     * @param deviceId the device ID
     * @param appCookie the group key
     *
     * @return a group associated with the key
     */
    @Override
    public Group getGroup(DeviceId deviceId, GroupKey appCookie) {
        return (groupEntriesByKey.get(deviceId) != null) ?
                      groupEntriesByKey.get(deviceId).get(appCookie) :
                      null;
    }

    private int getFreeGroupIdValue(DeviceId deviceId) {
        int freeId = groupIdGen.incrementAndGet();

        while (true) {
            Group existing = (
                    groupEntriesById.get(deviceId) != null) ?
                    groupEntriesById.get(deviceId).get(new DefaultGroupId(freeId)) :
                    null;
            if (existing == null) {
                existing = (
                        extraneousGroupEntriesById.get(deviceId) != null) ?
                        extraneousGroupEntriesById.get(deviceId).
                        get(new DefaultGroupId(freeId)) :
                        null;
            }
            if (existing != null) {
                freeId = groupIdGen.incrementAndGet();
            } else {
                break;
            }
        }
        return freeId;
    }

    /**
     * Stores a new group entry using the information from group description.
     *
     * @param groupDesc group description to be used to create group entry
     */
    @Override
    public void storeGroupDescription(GroupDescription groupDesc) {
        // Check if a group is existing with the same key
        if (getGroup(groupDesc.deviceId(), groupDesc.appCookie()) != null) {
            return;
        }

        if (deviceAuditStatus.get(groupDesc.deviceId()) == null) {
            // Device group audit has not completed yet
            // Add this group description to pending group key table
            // Create a group entry object with Dummy Group ID
            StoredGroupEntry group = new DefaultGroup(dummyGroupId, groupDesc);
            group.setState(GroupState.WAITING_AUDIT_COMPLETE);
            ConcurrentMap<GroupKey, StoredGroupEntry> pendingKeyTable =
                    getPendingGroupKeyTable(groupDesc.deviceId());
            pendingKeyTable.put(groupDesc.appCookie(), group);
            return;
        }

        storeGroupDescriptionInternal(groupDesc);
    }

    private void storeGroupDescriptionInternal(GroupDescription groupDesc) {
        // Get a new group identifier
        GroupId id = new DefaultGroupId(getFreeGroupIdValue(groupDesc.deviceId()));
        // Create a group entry object
        StoredGroupEntry group = new DefaultGroup(id, groupDesc);
        // Insert the newly created group entry into concurrent key and id maps
        ConcurrentMap<GroupKey, StoredGroupEntry> keyTable =
                getGroupKeyTable(groupDesc.deviceId());
        keyTable.put(groupDesc.appCookie(), group);
        ConcurrentMap<GroupId, StoredGroupEntry> idTable =
                getGroupIdTable(groupDesc.deviceId());
        idTable.put(id, group);
        notifyDelegate(new GroupEvent(GroupEvent.Type.GROUP_ADD_REQUESTED,
                                      group));
    }

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
    @Override
    public void updateGroupDescription(DeviceId deviceId,
                                GroupKey oldAppCookie,
                                UpdateType type,
                                GroupBuckets newBuckets,
                                GroupKey newAppCookie) {
        // Check if a group is existing with the provided key
        Group oldGroup = getGroup(deviceId, oldAppCookie);
        if (oldGroup == null) {
            return;
        }

        List<GroupBucket> newBucketList = getUpdatedBucketList(oldGroup,
                                                               type,
                                                               newBuckets);
        if (newBucketList != null) {
            // Create a new group object from the old group
            GroupBuckets updatedBuckets = new GroupBuckets(newBucketList);
            GroupKey newCookie = (newAppCookie != null) ? newAppCookie : oldAppCookie;
            GroupDescription updatedGroupDesc = new DefaultGroupDescription(
                                                        oldGroup.deviceId(),
                                                        oldGroup.type(),
                                                        updatedBuckets,
                                                        newCookie,
                                                        oldGroup.appId());
            StoredGroupEntry newGroup = new DefaultGroup(oldGroup.id(),
                                                     updatedGroupDesc);
            newGroup.setState(GroupState.PENDING_UPDATE);
            newGroup.setLife(oldGroup.life());
            newGroup.setPackets(oldGroup.packets());
            newGroup.setBytes(oldGroup.bytes());
            // Remove the old entry from maps and add new entry using new key
            ConcurrentMap<GroupKey, StoredGroupEntry> keyTable =
                    getGroupKeyTable(oldGroup.deviceId());
            ConcurrentMap<GroupId, StoredGroupEntry> idTable =
                    getGroupIdTable(oldGroup.deviceId());
            keyTable.remove(oldGroup.appCookie());
            idTable.remove(oldGroup.id());
            keyTable.put(newGroup.appCookie(), newGroup);
            idTable.put(newGroup.id(), newGroup);
            notifyDelegate(new GroupEvent(Type.GROUP_UPDATE_REQUESTED, newGroup));
        }
    }

    private List<GroupBucket> getUpdatedBucketList(Group oldGroup,
                                   UpdateType type,
                                   GroupBuckets buckets) {
        GroupBuckets oldBuckets = oldGroup.buckets();
        List<GroupBucket> newBucketList = new ArrayList<GroupBucket>(
                oldBuckets.buckets());
        boolean groupDescUpdated = false;

        if (type == UpdateType.ADD) {
            // Check if the any of the new buckets are part of
            // the old bucket list
            for (GroupBucket addBucket:buckets.buckets()) {
                if (!newBucketList.contains(addBucket)) {
                    newBucketList.add(addBucket);
                    groupDescUpdated = true;
                }
            }
        } else if (type == UpdateType.REMOVE) {
            // Check if the to be removed buckets are part of the
            // old bucket list
            for (GroupBucket removeBucket:buckets.buckets()) {
                if (newBucketList.contains(removeBucket)) {
                    newBucketList.remove(removeBucket);
                    groupDescUpdated = true;
                }
            }
        }

        if (groupDescUpdated) {
            return newBucketList;
        } else {
            return null;
        }
    }

    /**
     * Triggers deleting the existing group entry.
     *
     * @param deviceId the device ID
     * @param appCookie the group key
     */
    @Override
    public void deleteGroupDescription(DeviceId deviceId,
                                GroupKey appCookie) {
        // Check if a group is existing with the provided key
        StoredGroupEntry existing = (groupEntriesByKey.get(deviceId) != null) ?
                           groupEntriesByKey.get(deviceId).get(appCookie) :
                           null;
        if (existing == null) {
            return;
        }

        synchronized (existing) {
            existing.setState(GroupState.PENDING_DELETE);
        }
        notifyDelegate(new GroupEvent(Type.GROUP_REMOVE_REQUESTED, existing));
    }

    /**
     * Stores a new group entry, or updates an existing entry.
     *
     * @param group group entry
     */
    @Override
    public void addOrUpdateGroupEntry(Group group) {
        // check if this new entry is an update to an existing entry
        StoredGroupEntry existing = (groupEntriesById.get(
                      group.deviceId()) != null) ?
                      groupEntriesById.get(group.deviceId()).get(group.id()) :
                      null;
        GroupEvent event = null;

        if (existing != null) {
            synchronized (existing) {
                existing.setLife(group.life());
                existing.setPackets(group.packets());
                existing.setBytes(group.bytes());
                if (existing.state() == GroupState.PENDING_ADD) {
                    existing.setState(GroupState.ADDED);
                    event = new GroupEvent(Type.GROUP_ADDED, existing);
                } else {
                    if (existing.state() == GroupState.PENDING_UPDATE) {
                        existing.setState(GroupState.PENDING_UPDATE);
                    }
                    event = new GroupEvent(Type.GROUP_UPDATED, existing);
                }
            }
        }

        if (event != null) {
            notifyDelegate(event);
        }
    }

    /**
     * Removes the group entry from store.
     *
     * @param group group entry
     */
    @Override
    public void removeGroupEntry(Group group) {
        StoredGroupEntry existing = (groupEntriesById.get(
                         group.deviceId()) != null) ?
                         groupEntriesById.get(group.deviceId()).get(group.id()) :
                         null;

        if (existing != null) {
            ConcurrentMap<GroupKey, StoredGroupEntry> keyTable =
                    getGroupKeyTable(existing.deviceId());
            ConcurrentMap<GroupId, StoredGroupEntry> idTable =
                    getGroupIdTable(existing.deviceId());
            idTable.remove(existing.id());
            keyTable.remove(existing.appCookie());
            notifyDelegate(new GroupEvent(Type.GROUP_REMOVED, existing));
        }
    }

    @Override
    public void deviceInitialAuditCompleted(DeviceId deviceId) {
        synchronized (deviceAuditStatus) {
            deviceAuditStatus.putIfAbsent(deviceId, true);
            // Execute all pending group requests
            ConcurrentMap<GroupKey, StoredGroupEntry> pendingGroupRequests =
                    getPendingGroupKeyTable(deviceId);
            for (Group group:pendingGroupRequests.values()) {
                GroupDescription tmp = new DefaultGroupDescription(
                             group.deviceId(),
                             group.type(),
                             group.buckets(),
                             group.appCookie(),
                             group.appId());
                storeGroupDescriptionInternal(tmp);
            }
            getPendingGroupKeyTable(deviceId).clear();
        }
    }

    @Override
    public boolean deviceInitialAuditStatus(DeviceId deviceId) {
        synchronized (deviceAuditStatus) {
            return (deviceAuditStatus.get(deviceId) != null) ? true : false;
        }
    }

    @Override
    public void addOrUpdateExtraneousGroupEntry(Group group) {
        ConcurrentMap<GroupId, Group> extraneousIdTable =
                getExtraneousGroupIdTable(group.deviceId());
        extraneousIdTable.put(group.id(), group);
        // Check the reference counter
        if (group.referenceCount() == 0) {
            notifyDelegate(new GroupEvent(Type.GROUP_REMOVE_REQUESTED, group));
        }
    }

    @Override
    public void removeExtraneousGroupEntry(Group group) {
        ConcurrentMap<GroupId, Group> extraneousIdTable =
                getExtraneousGroupIdTable(group.deviceId());
        extraneousIdTable.remove(group.id());
    }

    @Override
    public Iterable<Group> getExtraneousGroups(DeviceId deviceId) {
        // flatten and make iterator unmodifiable
        return FluentIterable.from(
                  getExtraneousGroupIdTable(deviceId).values());
    }
}
