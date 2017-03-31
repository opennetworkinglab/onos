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
package org.onosproject.store.trivial;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
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
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupStore;
import org.onosproject.net.group.GroupStoreDelegate;
import org.onosproject.net.group.StoredGroupBucketEntry;
import org.onosproject.net.group.StoredGroupEntry;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;

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
    private final GroupId dummyGroupId = new GroupId(dummyId);

    // inner Map is per device group table
    private final ConcurrentMap<DeviceId, ConcurrentMap<GroupKey, StoredGroupEntry>>
            groupEntriesByKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, ConcurrentMap<GroupId, StoredGroupEntry>>
            groupEntriesById = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, ConcurrentMap<GroupKey, StoredGroupEntry>>
            pendingGroupEntriesByKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceId, ConcurrentMap<GroupId, Group>>
        extraneousGroupEntriesById = new ConcurrentHashMap<>();

    private final HashMap<DeviceId, Boolean> deviceAuditStatus = new HashMap<>();

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

    /**
     * Returns the group key table for specified device.
     *
     * @param deviceId identifier of the device
     * @return Map representing group key table of given device.
     */
    private ConcurrentMap<GroupKey, StoredGroupEntry> getGroupKeyTable(DeviceId deviceId) {
        return groupEntriesByKey.computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>());
    }

    /**
     * Returns the group id table for specified device.
     *
     * @param deviceId identifier of the device
     * @return Map representing group key table of given device.
     */
    private ConcurrentMap<GroupId, StoredGroupEntry> getGroupIdTable(DeviceId deviceId) {
        return groupEntriesById.computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>());
    }

    /**
     * Returns the pending group key table for specified device.
     *
     * @param deviceId identifier of the device
     * @return Map representing group key table of given device.
     */
    private ConcurrentMap<GroupKey, StoredGroupEntry>
                    getPendingGroupKeyTable(DeviceId deviceId) {
        return pendingGroupEntriesByKey.computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>());
    }

    /**
     * Returns the extraneous group id table for specified device.
     *
     * @param deviceId identifier of the device
     * @return Map representing group key table of given device.
     */
    private ConcurrentMap<GroupId, Group>
                getExtraneousGroupIdTable(DeviceId deviceId) {
        return extraneousGroupEntriesById.computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>());
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
            .transform(input -> input);
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

    @Override
    public Group getGroup(DeviceId deviceId, GroupId groupId) {
        return (groupEntriesById.get(deviceId) != null) ?
                      groupEntriesById.get(deviceId).get(groupId) :
                      null;
    }

    private int getFreeGroupIdValue(DeviceId deviceId) {
        int freeId = groupIdGen.incrementAndGet();

        while (true) {
            Group existing = (
                    groupEntriesById.get(deviceId) != null) ?
                    groupEntriesById.get(deviceId).get(new GroupId(freeId)) :
                    null;
            if (existing == null) {
                existing = (
                        extraneousGroupEntriesById.get(deviceId) != null) ?
                        extraneousGroupEntriesById.get(deviceId).
                        get(new GroupId(freeId)) :
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
        // Check if a group is existing with the same key
        if (getGroup(groupDesc.deviceId(), groupDesc.appCookie()) != null) {
            return;
        }

        GroupId id = null;
        if (groupDesc.givenGroupId() == null) {
            // Get a new group identifier
            id = new GroupId(getFreeGroupIdValue(groupDesc.deviceId()));
        } else {
            id = new GroupId(groupDesc.givenGroupId());
        }
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
                                                        oldGroup.givenGroupId(),
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
        if (type == UpdateType.SET) {
            return buckets.buckets();
        }

        List<GroupBucket> oldBuckets = oldGroup.buckets().buckets();
        List<GroupBucket> updatedBucketList = new ArrayList<>();
        boolean groupDescUpdated = false;

        if (type == UpdateType.ADD) {
            List<GroupBucket> newBuckets = buckets.buckets();

            // Add old buckets that will not be updated and check if any will be updated.
            for (GroupBucket oldBucket : oldBuckets) {
                int newBucketIndex = newBuckets.indexOf(oldBucket);

                if (newBucketIndex != -1) {
                    GroupBucket newBucket = newBuckets.get(newBucketIndex);
                    if (!newBucket.hasSameParameters(oldBucket)) {
                        // Bucket will be updated
                        groupDescUpdated = true;
                    }
                } else {
                    // Old bucket will remain the same - add it.
                    updatedBucketList.add(oldBucket);
                }
            }

            // Add all new buckets
            updatedBucketList.addAll(newBuckets);
            if (!oldBuckets.containsAll(newBuckets)) {
                groupDescUpdated = true;
            }

        } else if (type == UpdateType.REMOVE) {
            List<GroupBucket> bucketsToRemove = buckets.buckets();

            // Check which old buckets should remain
            for (GroupBucket oldBucket : oldBuckets) {
                if (!bucketsToRemove.contains(oldBucket)) {
                    updatedBucketList.add(oldBucket);
                } else {
                    groupDescUpdated = true;
                }
            }
        }

        if (groupDescUpdated) {
            return updatedBucketList;
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
                for (GroupBucket bucket:group.buckets().buckets()) {
                    Optional<GroupBucket> matchingBucket =
                            existing.buckets().buckets()
                            .stream()
                            .filter((existingBucket) -> (existingBucket.equals(bucket)))
                            .findFirst();
                    if (matchingBucket.isPresent()) {
                        ((StoredGroupBucketEntry) matchingBucket.
                                get()).setPackets(bucket.packets());
                        ((StoredGroupBucketEntry) matchingBucket.
                                get()).setBytes(bucket.bytes());
                    } else {
                        log.warn("addOrUpdateGroupEntry: No matching "
                                + "buckets to update stats");
                    }
                }
                existing.setLife(group.life());
                existing.setPackets(group.packets());
                existing.setBytes(group.bytes());
                if (existing.state() == GroupState.PENDING_ADD) {
                    existing.setState(GroupState.ADDED);
                    event = new GroupEvent(Type.GROUP_ADDED, existing);
                } else {
                    if (existing.state() == GroupState.PENDING_UPDATE) {
                        existing.setState(GroupState.ADDED);
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
    public void purgeGroupEntry(DeviceId deviceId) {
        Set<Map.Entry<GroupId, StoredGroupEntry>> entryPendingRemove =
                groupEntriesById.get(deviceId).entrySet();

        groupEntriesById.remove(deviceId);
        groupEntriesByKey.remove(deviceId);

        entryPendingRemove.forEach(entry -> {
            notifyDelegate(new GroupEvent(Type.GROUP_REMOVED, entry.getValue()));
        });
    }

    @Override
    public void purgeGroupEntries() {
        groupEntriesById.values().forEach(groupEntries -> {
            groupEntries.entrySet().forEach(entry -> {
                notifyDelegate(new GroupEvent(Type.GROUP_REMOVED, entry.getValue()));
            });
        });

        groupEntriesById.clear();
        groupEntriesByKey.clear();
    }

    @Override
    public void deviceInitialAuditCompleted(DeviceId deviceId,
                                            boolean completed) {
        synchronized (deviceAuditStatus) {
            if (completed) {
                log.debug("deviceInitialAuditCompleted: AUDIT "
                        + "completed for device {}", deviceId);
                deviceAuditStatus.put(deviceId, true);
                // Execute all pending group requests
                ConcurrentMap<GroupKey, StoredGroupEntry> pendingGroupRequests =
                        getPendingGroupKeyTable(deviceId);
                for (Group group:pendingGroupRequests.values()) {
                    GroupDescription tmp = new DefaultGroupDescription(
                                                                       group.deviceId(),
                                                                       group.type(),
                                                                       group.buckets(),
                                                                       group.appCookie(),
                                                                       group.givenGroupId(),
                                                                       group.appId());
                    storeGroupDescriptionInternal(tmp);
                }
                getPendingGroupKeyTable(deviceId).clear();
            } else {
                if (deviceAuditStatus.get(deviceId)) {
                    log.debug("deviceInitialAuditCompleted: Clearing AUDIT "
                            + "status for device {}", deviceId);
                    deviceAuditStatus.put(deviceId, false);
                }
            }
        }
    }

    @Override
    public boolean deviceInitialAuditStatus(DeviceId deviceId) {
        synchronized (deviceAuditStatus) {
            return (deviceAuditStatus.get(deviceId) != null)
                    ? deviceAuditStatus.get(deviceId) : false;
        }
    }

    @Override
    public void groupOperationFailed(DeviceId deviceId, GroupOperation operation) {

        StoredGroupEntry existing = (groupEntriesById.get(
                deviceId) != null) ?
                groupEntriesById.get(deviceId).get(operation.groupId()) :
                null;

        if (existing == null) {
            log.warn("No group entry with ID {} found ", operation.groupId());
            return;
        }

        switch (operation.opType()) {
        case ADD:
            notifyDelegate(new GroupEvent(Type.GROUP_ADD_FAILED, existing));
            break;
        case MODIFY:
            notifyDelegate(new GroupEvent(Type.GROUP_UPDATE_FAILED, existing));
            break;
        case DELETE:
            notifyDelegate(new GroupEvent(Type.GROUP_REMOVE_FAILED, existing));
            break;
        default:
            log.warn("Unknown group operation type {}", operation.opType());
        }

        ConcurrentMap<GroupKey, StoredGroupEntry> keyTable =
                getGroupKeyTable(existing.deviceId());
        ConcurrentMap<GroupId, StoredGroupEntry> idTable =
                getGroupIdTable(existing.deviceId());
        idTable.remove(existing.id());
        keyTable.remove(existing.appCookie());
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

    @Override
    public void pushGroupMetrics(DeviceId deviceId,
                                 Collection<Group> groupEntries) {
        boolean deviceInitialAuditStatus =
                deviceInitialAuditStatus(deviceId);
        Set<Group> southboundGroupEntries =
                Sets.newHashSet(groupEntries);
        Set<Group> storedGroupEntries =
                Sets.newHashSet(getGroups(deviceId));
        Set<Group> extraneousStoredEntries =
                Sets.newHashSet(getExtraneousGroups(deviceId));

        if (log.isTraceEnabled()) {
            log.trace("pushGroupMetrics: Displaying all ({}) "
                            + "southboundGroupEntries for device {}",
                    southboundGroupEntries.size(),
                    deviceId);
            for (Group group : southboundGroupEntries) {
                log.trace("Group {} in device {}", group, deviceId);
            }

            log.trace("Displaying all ({}) stored group entries for device {}",
                    storedGroupEntries.size(),
                    deviceId);
            for (Group group : storedGroupEntries) {
                log.trace("Stored Group {} for device {}", group, deviceId);
            }
        }

        for (Iterator<Group> it2 = southboundGroupEntries.iterator(); it2.hasNext();) {
            Group group = it2.next();
            if (storedGroupEntries.remove(group)) {
                // we both have the group, let's update some info then.
                log.trace("Group AUDIT: group {} exists "
                        + "in both planes for device {}",
                        group.id(), deviceId);
                groupAdded(group);
                it2.remove();
            }
        }
        for (Group group : southboundGroupEntries) {
            if (getGroup(group.deviceId(), group.id()) != null) {
                // There is a group existing with the same id
                // It is possible that group update is
                // in progress while we got a stale info from switch
                if (!storedGroupEntries.remove(getGroup(
                             group.deviceId(), group.id()))) {
                    log.warn("Group AUDIT: Inconsistent state:"
                            + "Group exists in ID based table while "
                            + "not present in key based table");
                }
            } else {
                // there are groups in the switch that aren't in the store
                log.trace("Group AUDIT: extraneous group {} exists "
                        + "in data plane for device {}",
                        group.id(), deviceId);
                extraneousStoredEntries.remove(group);
                extraneousGroup(group);
            }
        }
        for (Group group : storedGroupEntries) {
            // there are groups in the store that aren't in the switch
            log.trace("Group AUDIT: group {} missing "
                    + "in data plane for device {}",
                    group.id(), deviceId);
            groupMissing(group);
        }
        for (Group group : extraneousStoredEntries) {
            // there are groups in the extraneous store that
            // aren't in the switch
            log.trace("Group AUDIT: clearing extransoeus group {} "
                    + "from store for device {}",
                    group.id(), deviceId);
            removeExtraneousGroupEntry(group);
        }

        if (!deviceInitialAuditStatus) {
            log.debug("Group AUDIT: Setting device {} initial "
                    + "AUDIT completed", deviceId);
            deviceInitialAuditCompleted(deviceId, true);
        }
    }

    @Override
    public void notifyOfFailovers(Collection<Group> failoverGroups) {
        List<GroupEvent> failoverEvents = new ArrayList<>();
        failoverGroups.forEach(group -> {
            if (group.type() == Group.Type.FAILOVER) {
                failoverEvents.add(new GroupEvent(GroupEvent.Type.GROUP_BUCKET_FAILOVER, group));
            }
        });
        notifyDelegate(failoverEvents);
    }

    private void groupMissing(Group group) {
        switch (group.state()) {
            case PENDING_DELETE:
                log.debug("Group {} delete confirmation from device {}",
                          group, group.deviceId());
                removeGroupEntry(group);
                break;
            case ADDED:
            case PENDING_ADD:
            case PENDING_UPDATE:
                log.debug("Group {} is in store but not on device {}",
                          group, group.deviceId());
                StoredGroupEntry existing = (groupEntriesById.get(
                              group.deviceId()) != null) ?
                              groupEntriesById.get(group.deviceId()).get(group.id()) :
                              null;
                log.trace("groupMissing: group "
                        + "entry {} in device {} moving "
                        + "from {} to PENDING_ADD",
                        existing.id(),
                        existing.deviceId(),
                        existing.state());
                existing.setState(Group.GroupState.PENDING_ADD);
                notifyDelegate(new GroupEvent(GroupEvent.Type.GROUP_ADD_REQUESTED,
                                              group));
                break;
            default:
                log.debug("Group {} has not been installed.", group);
                break;
        }
    }

    private void extraneousGroup(Group group) {
        log.debug("Group {} is on device {} but not in store.",
                  group, group.deviceId());
        addOrUpdateExtraneousGroupEntry(group);
    }

    private void groupAdded(Group group) {
        log.trace("Group {} Added or Updated in device {}",
                  group, group.deviceId());
        addOrUpdateGroupEntry(group);
    }
}
