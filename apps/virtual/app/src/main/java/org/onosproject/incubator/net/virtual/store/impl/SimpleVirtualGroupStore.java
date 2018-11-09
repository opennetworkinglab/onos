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

package org.onosproject.incubator.net.virtual.store.impl;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import org.onosproject.core.GroupId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkGroupStore;
import org.onosproject.net.DeviceId;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupStoreDelegate;
import org.onosproject.net.group.StoredGroupBucketEntry;
import org.onosproject.net.group.StoredGroupEntry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

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

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of virtual group entries using trivial in-memory implementation.
 */
@Component(immediate = true, service = VirtualNetworkGroupStore.class)
public class SimpleVirtualGroupStore
        extends AbstractVirtualStore<GroupEvent, GroupStoreDelegate>
        implements VirtualNetworkGroupStore {

    private final Logger log = getLogger(getClass());

    private final int dummyId = 0xffffffff;
    private final GroupId dummyGroupId = new GroupId(dummyId);

    // inner Map is per device group table
    private final ConcurrentMap<NetworkId,
            ConcurrentMap<DeviceId, ConcurrentMap<GroupKey, StoredGroupEntry>>>
            groupEntriesByKey = new ConcurrentHashMap<>();

    private final ConcurrentMap<NetworkId,
            ConcurrentMap<DeviceId, ConcurrentMap<GroupId, StoredGroupEntry>>>
            groupEntriesById = new ConcurrentHashMap<>();

    private final ConcurrentMap<NetworkId,
            ConcurrentMap<DeviceId, ConcurrentMap<GroupKey, StoredGroupEntry>>>
            pendingGroupEntriesByKey = new ConcurrentHashMap<>();

    private final ConcurrentMap<NetworkId,
            ConcurrentMap<DeviceId, ConcurrentMap<GroupId, Group>>>
            extraneousGroupEntriesById = new ConcurrentHashMap<>();

    private final ConcurrentMap<NetworkId, HashMap<DeviceId, Boolean>>
            deviceAuditStatus = new ConcurrentHashMap<>();

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
     * @param networkId identifier of the virtual network
     * @param deviceId identifier of the device
     * @return Map representing group key table of given device.
     */
    private ConcurrentMap<GroupKey, StoredGroupEntry>
    getGroupKeyTable(NetworkId networkId, DeviceId deviceId) {
        groupEntriesByKey.computeIfAbsent(networkId, n -> new ConcurrentHashMap<>());
        return groupEntriesByKey.get(networkId)
                .computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>());
    }

    /**
     * Returns the group id table for specified device.
     *
     * @param networkId identifier of the virtual network
     * @param deviceId identifier of the device
     * @return Map representing group key table of given device.
     */
    private ConcurrentMap<GroupId, StoredGroupEntry>
    getGroupIdTable(NetworkId networkId, DeviceId deviceId) {
        groupEntriesById.computeIfAbsent(networkId, n -> new ConcurrentHashMap<>());
        return groupEntriesById.get(networkId)
                .computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>());
    }

    /**
     * Returns the pending group key table for specified device.
     *
     * @param networkId identifier of the virtual network
     * @param deviceId identifier of the device
     * @return Map representing group key table of given device.
     */
    private ConcurrentMap<GroupKey, StoredGroupEntry>
    getPendingGroupKeyTable(NetworkId networkId, DeviceId deviceId) {
        pendingGroupEntriesByKey.computeIfAbsent(networkId, n -> new ConcurrentHashMap<>());
        return pendingGroupEntriesByKey.get(networkId)
                .computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>());
    }

    /**
     * Returns the extraneous group id table for specified device.
     *
     * @param networkId identifier of the virtual network
     * @param deviceId identifier of the device
     * @return Map representing group key table of given device.
     */
    private ConcurrentMap<GroupId, Group>
    getExtraneousGroupIdTable(NetworkId networkId, DeviceId deviceId) {
        extraneousGroupEntriesById.computeIfAbsent(networkId, n -> new ConcurrentHashMap<>());
        return extraneousGroupEntriesById.get(networkId)
                .computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>());
    }

    @Override
    public int getGroupCount(NetworkId networkId, DeviceId deviceId) {
        return (groupEntriesByKey.get(networkId).get(deviceId) != null) ?
                groupEntriesByKey.get(networkId).get(deviceId).size() : 0;
    }

    @Override
    public Iterable<Group> getGroups(NetworkId networkId, DeviceId deviceId) {
        // flatten and make iterator unmodifiable
        return FluentIterable.from(getGroupKeyTable(networkId, deviceId).values())
                .transform(input -> input);
    }

    @Override
    public Group getGroup(NetworkId networkId, DeviceId deviceId, GroupKey appCookie) {
        if (groupEntriesByKey.get(networkId) != null &&
                groupEntriesByKey.get(networkId).get(deviceId) != null) {
            return groupEntriesByKey.get(networkId).get(deviceId).get(appCookie);
        }
        return null;
    }

    @Override
    public Group getGroup(NetworkId networkId, DeviceId deviceId, GroupId groupId) {
        if (groupEntriesById.get(networkId) != null &&
                groupEntriesById.get(networkId).get(deviceId) != null) {
            return groupEntriesById.get(networkId).get(deviceId).get(groupId);
        }
        return null;
    }

    private int getFreeGroupIdValue(NetworkId networkId, DeviceId deviceId) {
        int freeId = groupIdGen.incrementAndGet();

        while (true) {
            Group existing = null;
            if (groupEntriesById.get(networkId) != null &&
                    groupEntriesById.get(networkId).get(deviceId) != null) {
                existing = groupEntriesById.get(networkId).get(deviceId)
                                .get(new GroupId(freeId));
            }

            if (existing == null) {
                if (extraneousGroupEntriesById.get(networkId) != null &&
                        extraneousGroupEntriesById.get(networkId).get(deviceId) != null) {
                    existing = extraneousGroupEntriesById.get(networkId).get(deviceId)
                                    .get(new GroupId(freeId));
                }
            }

            if (existing != null) {
                freeId = groupIdGen.incrementAndGet();
            } else {
                break;
            }
        }
        return freeId;
    }

    @Override
    public void storeGroupDescription(NetworkId networkId, GroupDescription groupDesc) {
        // Check if a group is existing with the same key
        if (getGroup(networkId, groupDesc.deviceId(), groupDesc.appCookie()) != null) {
            return;
        }

        if (deviceAuditStatus.get(networkId) == null ||
                deviceAuditStatus.get(networkId).get(groupDesc.deviceId()) == null) {
            // Device group audit has not completed yet
            // Add this group description to pending group key table
            // Create a group entry object with Dummy Group ID
            StoredGroupEntry group = new DefaultGroup(dummyGroupId, groupDesc);
            group.setState(Group.GroupState.WAITING_AUDIT_COMPLETE);
            ConcurrentMap<GroupKey, StoredGroupEntry> pendingKeyTable =
                    getPendingGroupKeyTable(networkId, groupDesc.deviceId());
            pendingKeyTable.put(groupDesc.appCookie(), group);
            return;
        }

        storeGroupDescriptionInternal(networkId, groupDesc);
    }

    private void storeGroupDescriptionInternal(NetworkId networkId,
                                               GroupDescription groupDesc) {
        // Check if a group is existing with the same key
        if (getGroup(networkId, groupDesc.deviceId(), groupDesc.appCookie()) != null) {
            return;
        }

        GroupId id = null;
        if (groupDesc.givenGroupId() == null) {
            // Get a new group identifier
            id = new GroupId(getFreeGroupIdValue(networkId, groupDesc.deviceId()));
        } else {
            id = new GroupId(groupDesc.givenGroupId());
        }
        // Create a group entry object
        StoredGroupEntry group = new DefaultGroup(id, groupDesc);
        // Insert the newly created group entry into concurrent key and id maps
        ConcurrentMap<GroupKey, StoredGroupEntry> keyTable =
                getGroupKeyTable(networkId, groupDesc.deviceId());
        keyTable.put(groupDesc.appCookie(), group);
        ConcurrentMap<GroupId, StoredGroupEntry> idTable =
                getGroupIdTable(networkId, groupDesc.deviceId());
        idTable.put(id, group);
        notifyDelegate(networkId, new GroupEvent(GroupEvent.Type.GROUP_ADD_REQUESTED,
                                      group));
    }

    @Override
    public void updateGroupDescription(NetworkId networkId, DeviceId deviceId,
                                       GroupKey oldAppCookie, UpdateType type,
                                       GroupBuckets newBuckets, GroupKey newAppCookie) {
        // Check if a group is existing with the provided key
        Group oldGroup = getGroup(networkId, deviceId, oldAppCookie);
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
            newGroup.setState(Group.GroupState.PENDING_UPDATE);
            newGroup.setLife(oldGroup.life());
            newGroup.setPackets(oldGroup.packets());
            newGroup.setBytes(oldGroup.bytes());

            // Remove the old entry from maps and add new entry using new key
            ConcurrentMap<GroupKey, StoredGroupEntry> keyTable =
                    getGroupKeyTable(networkId, oldGroup.deviceId());
            ConcurrentMap<GroupId, StoredGroupEntry> idTable =
                    getGroupIdTable(networkId, oldGroup.deviceId());
            keyTable.remove(oldGroup.appCookie());
            idTable.remove(oldGroup.id());
            keyTable.put(newGroup.appCookie(), newGroup);
            idTable.put(newGroup.id(), newGroup);
            notifyDelegate(networkId,
                           new GroupEvent(GroupEvent.Type.GROUP_UPDATE_REQUESTED,
                                          newGroup));
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

    @Override
    public void deleteGroupDescription(NetworkId networkId, DeviceId deviceId,
                                       GroupKey appCookie) {
        // Check if a group is existing with the provided key
        StoredGroupEntry existing = null;
        if (groupEntriesByKey.get(networkId) != null &&
                groupEntriesByKey.get(networkId).get(deviceId) != null) {
            existing = groupEntriesByKey.get(networkId).get(deviceId).get(appCookie);
        }

        if (existing == null) {
            return;
        }

        synchronized (existing) {
            existing.setState(Group.GroupState.PENDING_DELETE);
        }
        notifyDelegate(networkId,
                       new GroupEvent(GroupEvent.Type.GROUP_REMOVE_REQUESTED, existing));
    }

    @Override
    public void addOrUpdateGroupEntry(NetworkId networkId, Group group) {
        // check if this new entry is an update to an existing entry
        StoredGroupEntry existing = null;

        if (groupEntriesById.get(networkId) != null &&
                groupEntriesById.get(networkId).get(group.deviceId()) != null) {
            existing = groupEntriesById
                    .get(networkId)
                    .get(group.deviceId())
                    .get(group.id());
        }

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
                if (existing.state() == Group.GroupState.PENDING_ADD) {
                    existing.setState(Group.GroupState.ADDED);
                    event = new GroupEvent(GroupEvent.Type.GROUP_ADDED, existing);
                } else {
                    if (existing.state() == Group.GroupState.PENDING_UPDATE) {
                        existing.setState(Group.GroupState.ADDED);
                    }
                    event = new GroupEvent(GroupEvent.Type.GROUP_UPDATED, existing);
                }
            }
        }

        if (event != null) {
            notifyDelegate(networkId, event);
        }
    }

    @Override
    public void removeGroupEntry(NetworkId networkId, Group group) {
        StoredGroupEntry existing = null;
        if (groupEntriesById.get(networkId) != null
                && groupEntriesById.get(networkId).get(group.deviceId()) != null) {
           existing = groupEntriesById
                   .get(networkId).get(group.deviceId()).get(group.id());
        }

        if (existing != null) {
            ConcurrentMap<GroupKey, StoredGroupEntry> keyTable =
                    getGroupKeyTable(networkId, existing.deviceId());
            ConcurrentMap<GroupId, StoredGroupEntry> idTable =
                    getGroupIdTable(networkId, existing.deviceId());
            idTable.remove(existing.id());
            keyTable.remove(existing.appCookie());
            notifyDelegate(networkId,
                           new GroupEvent(GroupEvent.Type.GROUP_REMOVED, existing));
        }
    }

    @Override
    public void purgeGroupEntry(NetworkId networkId, DeviceId deviceId) {
        if (groupEntriesById.get(networkId) != null) {
            Set<Map.Entry<GroupId, StoredGroupEntry>> entryPendingRemove =
                    groupEntriesById.get(networkId).get(deviceId).entrySet();
            groupEntriesById.get(networkId).remove(deviceId);
            groupEntriesByKey.get(networkId).remove(deviceId);

            entryPendingRemove.forEach(entry -> {
                notifyDelegate(networkId,
                               new GroupEvent(GroupEvent.Type.GROUP_REMOVED,
                                              entry.getValue()));
            });
        }
    }

    @Override
    public void purgeGroupEntries(NetworkId networkId) {
        if (groupEntriesById.get(networkId) != null) {
            groupEntriesById.get((networkId)).values().forEach(groupEntries -> {
                groupEntries.entrySet().forEach(entry -> {
                    notifyDelegate(networkId,
                                   new GroupEvent(GroupEvent.Type.GROUP_REMOVED,
                                                  entry.getValue()));
                });
            });

            groupEntriesById.get(networkId).clear();
            groupEntriesByKey.get(networkId).clear();
        }
    }

    @Override
    public void addOrUpdateExtraneousGroupEntry(NetworkId networkId, Group group) {
        ConcurrentMap<GroupId, Group> extraneousIdTable =
                getExtraneousGroupIdTable(networkId, group.deviceId());
        extraneousIdTable.put(group.id(), group);
        // Check the reference counter
        if (group.referenceCount() == 0) {
            notifyDelegate(networkId,
                           new GroupEvent(GroupEvent.Type.GROUP_REMOVE_REQUESTED, group));
        }
    }

    @Override
    public void removeExtraneousGroupEntry(NetworkId networkId, Group group) {
        ConcurrentMap<GroupId, Group> extraneousIdTable =
                getExtraneousGroupIdTable(networkId, group.deviceId());
        extraneousIdTable.remove(group.id());
    }

    @Override
    public Iterable<Group> getExtraneousGroups(NetworkId networkId, DeviceId deviceId) {
        // flatten and make iterator unmodifiable
        return FluentIterable.from(
                getExtraneousGroupIdTable(networkId, deviceId).values());
    }

    @Override
    public void deviceInitialAuditCompleted(NetworkId networkId, DeviceId deviceId,
                                            boolean completed) {
        deviceAuditStatus.computeIfAbsent(networkId, k -> new HashMap<>());

        HashMap<DeviceId, Boolean> deviceAuditStatusByNetwork =
                deviceAuditStatus.get(networkId);

        synchronized (deviceAuditStatusByNetwork) {
            if (completed) {
                log.debug("deviceInitialAuditCompleted: AUDIT "
                                  + "completed for device {}", deviceId);
                deviceAuditStatusByNetwork.put(deviceId, true);
                // Execute all pending group requests
                ConcurrentMap<GroupKey, StoredGroupEntry> pendingGroupRequests =
                        getPendingGroupKeyTable(networkId, deviceId);
                for (Group group:pendingGroupRequests.values()) {
                    GroupDescription tmp = new DefaultGroupDescription(
                            group.deviceId(),
                            group.type(),
                            group.buckets(),
                            group.appCookie(),
                            group.givenGroupId(),
                            group.appId());
                    storeGroupDescriptionInternal(networkId, tmp);
                }
                getPendingGroupKeyTable(networkId, deviceId).clear();
            } else {
                if (deviceAuditStatusByNetwork.get(deviceId)) {
                    log.debug("deviceInitialAuditCompleted: Clearing AUDIT "
                                      + "status for device {}", deviceId);
                    deviceAuditStatusByNetwork.put(deviceId, false);
                }
            }
        }
    }

    @Override
    public boolean deviceInitialAuditStatus(NetworkId networkId, DeviceId deviceId) {
        deviceAuditStatus.computeIfAbsent(networkId, k -> new HashMap<>());

        HashMap<DeviceId, Boolean> deviceAuditStatusByNetwork =
                deviceAuditStatus.get(networkId);

        synchronized (deviceAuditStatusByNetwork) {
            return (deviceAuditStatusByNetwork.get(deviceId) != null)
                    ? deviceAuditStatusByNetwork.get(deviceId) : false;
        }
    }

    @Override
    public void groupOperationFailed(NetworkId networkId, DeviceId deviceId,
                                     GroupOperation operation) {

        StoredGroupEntry existing = null;
        if (groupEntriesById.get(networkId) != null &&
                groupEntriesById.get(networkId).get(deviceId) != null) {
            existing = groupEntriesById.get(networkId).get(deviceId)
                    .get(operation.groupId());
        }

        if (existing == null) {
            log.warn("No group entry with ID {} found ", operation.groupId());
            return;
        }

        switch (operation.opType()) {
            case ADD:
                notifyDelegate(networkId,
                               new GroupEvent(GroupEvent.Type.GROUP_ADD_FAILED,
                                              existing));
                break;
            case MODIFY:
                notifyDelegate(networkId,
                               new GroupEvent(GroupEvent.Type.GROUP_UPDATE_FAILED,
                                              existing));
                break;
            case DELETE:
                notifyDelegate(networkId,
                               new GroupEvent(GroupEvent.Type.GROUP_REMOVE_FAILED,
                                              existing));
                break;
            default:
                log.warn("Unknown group operation type {}", operation.opType());
        }

        ConcurrentMap<GroupKey, StoredGroupEntry> keyTable =
                getGroupKeyTable(networkId, existing.deviceId());
        ConcurrentMap<GroupId, StoredGroupEntry> idTable =
                getGroupIdTable(networkId, existing.deviceId());
        idTable.remove(existing.id());
        keyTable.remove(existing.appCookie());
    }

    @Override
    public void pushGroupMetrics(NetworkId networkId, DeviceId deviceId,
                                 Collection<Group> groupEntries) {
        boolean deviceInitialAuditStatus =
                deviceInitialAuditStatus(networkId, deviceId);
        Set<Group> southboundGroupEntries =
                Sets.newHashSet(groupEntries);
        Set<Group> storedGroupEntries =
                Sets.newHashSet(getGroups(networkId, deviceId));
        Set<Group> extraneousStoredEntries =
                Sets.newHashSet(getExtraneousGroups(networkId, deviceId));

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
                groupAdded(networkId, group);
                it2.remove();
            }
        }
        for (Group group : southboundGroupEntries) {
            if (getGroup(networkId, group.deviceId(), group.id()) != null) {
                // There is a group existing with the same id
                // It is possible that group update is
                // in progress while we got a stale info from switch
                if (!storedGroupEntries.remove(getGroup(
                        networkId, group.deviceId(), group.id()))) {
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
                extraneousGroup(networkId, group);
            }
        }
        for (Group group : storedGroupEntries) {
            // there are groups in the store that aren't in the switch
            log.trace("Group AUDIT: group {} missing "
                              + "in data plane for device {}",
                      group.id(), deviceId);
            groupMissing(networkId, group);
        }
        for (Group group : extraneousStoredEntries) {
            // there are groups in the extraneous store that
            // aren't in the switch
            log.trace("Group AUDIT: clearing extransoeus group {} "
                              + "from store for device {}",
                      group.id(), deviceId);
            removeExtraneousGroupEntry(networkId, group);
        }

        if (!deviceInitialAuditStatus) {
            log.debug("Group AUDIT: Setting device {} initial "
                              + "AUDIT completed", deviceId);
            deviceInitialAuditCompleted(networkId, deviceId, true);
        }
    }

    @Override
    public void notifyOfFailovers(NetworkId networkId, Collection<Group> failoverGroups) {
        List<GroupEvent> failoverEvents = new ArrayList<>();
        failoverGroups.forEach(group -> {
            if (group.type() == Group.Type.FAILOVER) {
                failoverEvents.add(new GroupEvent(GroupEvent.Type.GROUP_BUCKET_FAILOVER, group));
            }
        });
        notifyDelegate(networkId, failoverEvents);
    }

    private void groupMissing(NetworkId networkId, Group group) {
        switch (group.state()) {
            case PENDING_DELETE:
                log.debug("Group {} delete confirmation from device {} " +
                                  "of virtaual network {}",
                          group, group.deviceId(), networkId);
                removeGroupEntry(networkId, group);
                break;
            case ADDED:
            case PENDING_ADD:
            case PENDING_UPDATE:
                log.debug("Group {} is in store but not on device {}",
                          group, group.deviceId());
                StoredGroupEntry existing = null;
                if (groupEntriesById.get(networkId) != null &&
                        groupEntriesById.get(networkId).get(group.deviceId()) != null) {

                    existing = groupEntriesById.get(networkId)
                            .get(group.deviceId()).get(group.id());
                }
                if (existing == null) {
                    break;
                }

                log.trace("groupMissing: group "
                                  + "entry {} in device {} moving "
                                  + "from {} to PENDING_ADD",
                          existing.id(),
                          existing.deviceId(),
                          existing.state());
                existing.setState(Group.GroupState.PENDING_ADD);
                notifyDelegate(networkId, new GroupEvent(GroupEvent.Type.GROUP_ADD_REQUESTED,
                                              group));
                break;
            default:
                log.debug("Virtual network {} : Group {} has not been installed.",
                          networkId, group);
                break;
        }
    }

    private void extraneousGroup(NetworkId networkId, Group group) {
        log.debug("Group {} is on device {} of virtual network{}, but not in store.",
                  group, group.deviceId(), networkId);
        addOrUpdateExtraneousGroupEntry(networkId, group);
    }

    private void groupAdded(NetworkId networkId, Group group) {
        log.trace("Group {} Added or Updated in device {} of virtual network {}",
                  group, group.deviceId(), networkId);
        addOrUpdateGroupEntry(networkId, group);
    }
}
