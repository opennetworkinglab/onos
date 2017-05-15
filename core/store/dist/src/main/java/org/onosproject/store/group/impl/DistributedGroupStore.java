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
package org.onosproject.store.group.impl;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.GroupId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.group.DefaultGroup;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
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
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Topic;
import org.onosproject.store.service.Versioned;
import org.onosproject.store.service.DistributedPrimitive.Status;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages inventory of group entries using distributed group stores from the
 * storage service.
 */
@Component(immediate = true)
@Service
public class DistributedGroupStore
        extends AbstractStore<GroupEvent, GroupStoreDelegate>
        implements GroupStore {

    private final Logger log = getLogger(getClass());

    private static final boolean GARBAGE_COLLECT = false;
    private static final int GC_THRESH = 6;
    private static final boolean ALLOW_EXTRANEOUS_GROUPS = true;

    private final int dummyId = 0xffffffff;
    private final GroupId dummyGroupId = new GroupId(dummyId);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService clusterCommunicator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private ScheduledExecutorService executor;
    private Consumer<Status> statusChangeListener;
    // Per device group table with (device id + app cookie) as key
    private ConsistentMap<GroupStoreKeyMapKey,
            StoredGroupEntry> groupStoreEntriesByKey = null;
    // Per device group table with (device id + group id) as key
    private final ConcurrentMap<DeviceId, ConcurrentMap<GroupId, StoredGroupEntry>>
            groupEntriesById = new ConcurrentHashMap<>();
    private ConsistentMap<GroupStoreKeyMapKey,
            StoredGroupEntry> auditPendingReqQueue = null;
    private MapEventListener<GroupStoreKeyMapKey, StoredGroupEntry>
            mapListener = new GroupStoreKeyMapListener();
    private final ConcurrentMap<DeviceId, ConcurrentMap<GroupId, Group>>
            extraneousGroupEntriesById = new ConcurrentHashMap<>();
    private ExecutorService messageHandlingExecutor;
    private static final int MESSAGE_HANDLER_THREAD_POOL_SIZE = 1;
    private final HashMap<DeviceId, Boolean> deviceAuditStatus = new HashMap<>();

    private final AtomicInteger groupIdGen = new AtomicInteger();

    private KryoNamespace clusterMsgSerializer;

    private static Topic<GroupStoreMessage> groupTopic;

    @Property(name = "garbageCollect", boolValue = GARBAGE_COLLECT,
            label = "Enable group garbage collection")
    private boolean garbageCollect = GARBAGE_COLLECT;

    @Property(name = "gcThresh", intValue = GC_THRESH,
            label = "Number of rounds for group garbage collection")
    private int gcThresh = GC_THRESH;

    @Property(name = "allowExtraneousGroups", boolValue = ALLOW_EXTRANEOUS_GROUPS,
            label = "Allow groups in switches not installed by ONOS")
    private boolean allowExtraneousGroups = ALLOW_EXTRANEOUS_GROUPS;

    @Activate
    public void activate() {
        cfgService.registerProperties(getClass());
        KryoNamespace.Builder kryoBuilder = new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                .register(DefaultGroup.class,
                          DefaultGroupBucket.class,
                          DefaultGroupDescription.class,
                          DefaultGroupKey.class,
                          GroupDescription.Type.class,
                          Group.GroupState.class,
                          GroupBuckets.class,
                          GroupStoreMessage.class,
                          GroupStoreMessage.Type.class,
                          UpdateType.class,
                          GroupStoreMessageSubjects.class,
                          MultiValuedTimestamp.class,
                          GroupStoreKeyMapKey.class,
                          GroupStoreIdMapKey.class,
                          GroupStoreMapKey.class
                );

        clusterMsgSerializer = kryoBuilder.build("GroupStore");
        Serializer serializer = Serializer.using(clusterMsgSerializer);

        messageHandlingExecutor = Executors.
                newFixedThreadPool(MESSAGE_HANDLER_THREAD_POOL_SIZE,
                                   groupedThreads("onos/store/group",
                                                  "message-handlers",
                                                  log));

        clusterCommunicator.addSubscriber(GroupStoreMessageSubjects.REMOTE_GROUP_OP_REQUEST,
                                          clusterMsgSerializer::deserialize,
                                          this::process,
                                          messageHandlingExecutor);

        log.debug("Creating Consistent map onos-group-store-keymap");

        groupStoreEntriesByKey = storageService.<GroupStoreKeyMapKey, StoredGroupEntry>consistentMapBuilder()
                .withName("onos-group-store-keymap")
                .withSerializer(serializer)
                .build();
        groupStoreEntriesByKey.addListener(mapListener);
        log.debug("Current size of groupstorekeymap:{}",
                  groupStoreEntriesByKey.size());
        synchronizeGroupStoreEntries();

        log.debug("Creating GroupStoreId Map From GroupStoreKey Map");
        matchGroupEntries();
        executor = newSingleThreadScheduledExecutor(groupedThreads("onos/group", "store", log));
        statusChangeListener = status -> {
            if (status == Status.ACTIVE) {
                executor.execute(this::matchGroupEntries);
            }
        };
        groupStoreEntriesByKey.addStatusChangeListener(statusChangeListener);

        log.debug("Creating Consistent map pendinggroupkeymap");

        auditPendingReqQueue = storageService.<GroupStoreKeyMapKey, StoredGroupEntry>consistentMapBuilder()
                .withName("onos-pending-group-keymap")
                .withSerializer(serializer)
                .build();
        log.debug("Current size of pendinggroupkeymap:{}",
                  auditPendingReqQueue.size());

        groupTopic = getOrCreateGroupTopic(serializer);
        groupTopic.subscribe(this::processGroupMessage);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        groupStoreEntriesByKey.removeListener(mapListener);
        cfgService.unregisterProperties(getClass(), false);
        clusterCommunicator.removeSubscriber(GroupStoreMessageSubjects.REMOTE_GROUP_OP_REQUEST);
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();

        try {
            String s = get(properties, "garbageCollect");
            garbageCollect = isNullOrEmpty(s) ? GARBAGE_COLLECT : Boolean.parseBoolean(s.trim());

            s = get(properties, "gcThresh");
            gcThresh = isNullOrEmpty(s) ? GC_THRESH : Integer.parseInt(s.trim());

            s = get(properties, "allowExtraneousGroups");
            allowExtraneousGroups = isNullOrEmpty(s) ? ALLOW_EXTRANEOUS_GROUPS : Boolean.parseBoolean(s.trim());
        } catch (Exception e) {
            gcThresh = GC_THRESH;
            garbageCollect = GARBAGE_COLLECT;
            allowExtraneousGroups = ALLOW_EXTRANEOUS_GROUPS;
        }
    }

    private Topic<GroupStoreMessage> getOrCreateGroupTopic(Serializer serializer) {
        if (groupTopic == null) {
            return storageService.getTopic("group-failover-notif", serializer);
        } else {
            return groupTopic;
        }
    }

    /**
     * Updating values of groupEntriesById.
     */
    private void matchGroupEntries() {
        for (Entry<GroupStoreKeyMapKey, StoredGroupEntry> entry : groupStoreEntriesByKey.asJavaMap().entrySet()) {
            StoredGroupEntry group = entry.getValue();
            getGroupIdTable(entry.getKey().deviceId()).put(group.id(), group);
        }
    }


    private void synchronizeGroupStoreEntries() {
        Map<GroupStoreKeyMapKey, StoredGroupEntry> groupEntryMap = groupStoreEntriesByKey.asJavaMap();
        for (Entry<GroupStoreKeyMapKey, StoredGroupEntry> entry : groupEntryMap.entrySet()) {
            StoredGroupEntry value = entry.getValue();
            ConcurrentMap<GroupId, StoredGroupEntry> groupIdTable = getGroupIdTable(value.deviceId());
            groupIdTable.put(value.id(), value);
        }
    }

    /**
     * Returns the group store eventual consistent key map.
     *
     * @return Map representing group key table.
     */
    private Map<GroupStoreKeyMapKey, StoredGroupEntry>
    getGroupStoreKeyMap() {
        return groupStoreEntriesByKey.asJavaMap();
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
     * Returns the pending group request table.
     *
     * @return Map representing group key table.
     */
    private Map<GroupStoreKeyMapKey, StoredGroupEntry>
    getPendingGroupKeyTable() {
        return auditPendingReqQueue.asJavaMap();
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
        return (getGroups(deviceId) != null) ?
                Iterables.size(getGroups(deviceId)) : 0;
    }

    /**
     * Returns the groups associated with a device.
     *
     * @param deviceId the device ID
     * @return the group entries
     */
    @Override
    public Iterable<Group> getGroups(DeviceId deviceId) {
        // Let ImmutableSet.copyOf do the type conversion
        return ImmutableSet.copyOf(getStoredGroups(deviceId));
    }

    private Iterable<StoredGroupEntry> getStoredGroups(DeviceId deviceId) {
        NodeId master = mastershipService.getMasterFor(deviceId);
        if (master == null) {
            log.debug("Failed to getGroups: No master for {}", deviceId);
            return Collections.emptySet();
        }

        Set<StoredGroupEntry> storedGroups = getGroupStoreKeyMap().values()
                .stream()
                .filter(input -> input.deviceId().equals(deviceId))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(storedGroups);
    }

    /**
     * Returns the stored group entry.
     *
     * @param deviceId  the device ID
     * @param appCookie the group key
     * @return a group associated with the key
     */
    @Override
    public Group getGroup(DeviceId deviceId, GroupKey appCookie) {
        return getStoredGroupEntry(deviceId, appCookie);
    }

    private StoredGroupEntry getStoredGroupEntry(DeviceId deviceId,
                                                 GroupKey appCookie) {
        return getGroupStoreKeyMap().get(new GroupStoreKeyMapKey(deviceId,
                                                                 appCookie));
    }

    @Override
    public Group getGroup(DeviceId deviceId, GroupId groupId) {
        return getStoredGroupEntry(deviceId, groupId);
    }

    private StoredGroupEntry getStoredGroupEntry(DeviceId deviceId,
                                                 GroupId groupId) {
        return getGroupIdTable(deviceId).get(groupId);
    }

    private int getFreeGroupIdValue(DeviceId deviceId) {
        int freeId = groupIdGen.incrementAndGet();

        while (true) {
            Group existing = getGroup(deviceId, new GroupId(freeId));
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
        log.debug("getFreeGroupIdValue: Next Free ID is {}", freeId);
        return freeId;
    }

    /**
     * Stores a new group entry using the information from group description.
     *
     * @param groupDesc group description to be used to create group entry
     */
    @Override
    public void storeGroupDescription(GroupDescription groupDesc) {
        log.debug("In storeGroupDescription");
        // Check if a group is existing with the same key
        Group existingGroup = getGroup(groupDesc.deviceId(), groupDesc.appCookie());
        if (existingGroup != null) {
            log.info("Group already exists with the same key {} in dev:{} with id:0x{}",
                     groupDesc.appCookie(), groupDesc.deviceId(),
                     Integer.toHexString(existingGroup.id().id()));
            return;
        }

        // Check if group to be created by a remote instance
        if (mastershipService.getLocalRole(groupDesc.deviceId()) != MastershipRole.MASTER) {
            log.debug("storeGroupDescription: Device {} local role is not MASTER",
                      groupDesc.deviceId());
            if (mastershipService.getMasterFor(groupDesc.deviceId()) == null) {
                log.error("No Master for device {}..."
                                  + "Can not perform add group operation",
                          groupDesc.deviceId());
                //TODO: Send Group operation failure event
                return;
            }
            GroupStoreMessage groupOp = GroupStoreMessage.
                    createGroupAddRequestMsg(groupDesc.deviceId(),
                                             groupDesc);

            clusterCommunicator.unicast(groupOp,
                                        GroupStoreMessageSubjects.REMOTE_GROUP_OP_REQUEST,
                                        clusterMsgSerializer::serialize,
                                        mastershipService.getMasterFor(groupDesc.deviceId()))
                    .whenComplete((result, error) -> {
                        if (error != null) {
                            log.warn("Failed to send request to master: {} to {}",
                                     groupOp,
                                     mastershipService.getMasterFor(groupDesc.deviceId()));
                            //TODO: Send Group operation failure event
                        } else {
                            log.debug("Sent Group operation request for device {} "
                                              + "to remote MASTER {}",
                                      groupDesc.deviceId(),
                                      mastershipService.getMasterFor(groupDesc.deviceId()));
                        }
                    });
            return;
        }

        log.debug("Store group for device {} is getting handled locally",
                  groupDesc.deviceId());
        storeGroupDescriptionInternal(groupDesc);
    }

    private Group getMatchingExtraneousGroupbyId(DeviceId deviceId, Integer groupId) {
        ConcurrentMap<GroupId, Group> extraneousMap =
                extraneousGroupEntriesById.get(deviceId);
        if (extraneousMap == null) {
            return null;
        }
        return extraneousMap.get(new GroupId(groupId));
    }

    private Group getMatchingExtraneousGroupbyBuckets(DeviceId deviceId,
                                                      GroupBuckets buckets) {
        ConcurrentMap<GroupId, Group> extraneousMap =
                extraneousGroupEntriesById.get(deviceId);
        if (extraneousMap == null) {
            return null;
        }

        for (Group extraneousGroup : extraneousMap.values()) {
            if (extraneousGroup.buckets().equals(buckets)) {
                return extraneousGroup;
            }
        }
        return null;
    }

    private void storeGroupDescriptionInternal(GroupDescription groupDesc) {
        // Check if a group is existing with the same key
        if (getGroup(groupDesc.deviceId(), groupDesc.appCookie()) != null) {
            return;
        }

        if (deviceAuditStatus.get(groupDesc.deviceId()) == null) {
            // Device group audit has not completed yet
            // Add this group description to pending group key table
            // Create a group entry object with Dummy Group ID
            log.debug("storeGroupDescriptionInternal: Device {} AUDIT pending...Queuing Group ADD request",
                      groupDesc.deviceId());
            StoredGroupEntry group = new DefaultGroup(dummyGroupId, groupDesc);
            group.setState(GroupState.WAITING_AUDIT_COMPLETE);
            Map<GroupStoreKeyMapKey, StoredGroupEntry> pendingKeyTable =
                    getPendingGroupKeyTable();
            pendingKeyTable.put(new GroupStoreKeyMapKey(groupDesc.deviceId(),
                                                        groupDesc.appCookie()),
                                group);
            return;
        }

        Group matchingExtraneousGroup = null;
        if (groupDesc.givenGroupId() != null) {
            //Check if there is a extraneous group existing with the same Id
            matchingExtraneousGroup = getMatchingExtraneousGroupbyId(
                    groupDesc.deviceId(), groupDesc.givenGroupId());
            if (matchingExtraneousGroup != null) {
                log.debug("storeGroupDescriptionInternal: Matching extraneous group "
                                  + "found in Device {} for group id 0x{}",
                          groupDesc.deviceId(),
                          Integer.toHexString(groupDesc.givenGroupId()));
                //Check if the group buckets matches with user provided buckets
                if (matchingExtraneousGroup.buckets().equals(groupDesc.buckets())) {
                    //Group is already existing with the same buckets and Id
                    // Create a group entry object
                    log.debug("storeGroupDescriptionInternal: Buckets also matching "
                                      + "in Device {} for group id 0x{}",
                              groupDesc.deviceId(),
                              Integer.toHexString(groupDesc.givenGroupId()));
                    StoredGroupEntry group = new DefaultGroup(
                            matchingExtraneousGroup.id(), groupDesc);
                    // Insert the newly created group entry into key and id maps
                    getGroupStoreKeyMap().
                            put(new GroupStoreKeyMapKey(groupDesc.deviceId(),
                                                        groupDesc.appCookie()), group);
                    // Ensure it also inserted into group id based table to
                    // avoid any chances of duplication in group id generation
                    getGroupIdTable(groupDesc.deviceId()).
                            put(matchingExtraneousGroup.id(), group);
                    addOrUpdateGroupEntry(matchingExtraneousGroup);
                    removeExtraneousGroupEntry(matchingExtraneousGroup);
                    return;
                } else {
                    //Group buckets are not matching. Update group
                    //with user provided buckets.
                    log.debug("storeGroupDescriptionInternal: Buckets are not "
                                      + "matching in Device {} for group id 0x{}",
                              groupDesc.deviceId(),
                              Integer.toHexString(groupDesc.givenGroupId()));
                    StoredGroupEntry modifiedGroup = new DefaultGroup(
                            matchingExtraneousGroup.id(), groupDesc);
                    modifiedGroup.setState(GroupState.PENDING_UPDATE);
                    getGroupStoreKeyMap().
                            put(new GroupStoreKeyMapKey(groupDesc.deviceId(),
                                                        groupDesc.appCookie()), modifiedGroup);
                    // Ensure it also inserted into group id based table to
                    // avoid any chances of duplication in group id generation
                    getGroupIdTable(groupDesc.deviceId()).
                            put(matchingExtraneousGroup.id(), modifiedGroup);
                    removeExtraneousGroupEntry(matchingExtraneousGroup);
                    log.debug("storeGroupDescriptionInternal: Triggering Group "
                                      + "UPDATE request for {} in device {}",
                              matchingExtraneousGroup.id(),
                              groupDesc.deviceId());
                    notifyDelegate(new GroupEvent(Type.GROUP_UPDATE_REQUESTED, modifiedGroup));
                    return;
                }
            }
        } else {
            //Check if there is an extraneous group with user provided buckets
            matchingExtraneousGroup = getMatchingExtraneousGroupbyBuckets(
                    groupDesc.deviceId(), groupDesc.buckets());
            if (matchingExtraneousGroup != null) {
                //Group is already existing with the same buckets.
                //So reuse this group.
                log.debug("storeGroupDescriptionInternal: Matching extraneous group found in Device {}",
                          groupDesc.deviceId());
                //Create a group entry object
                StoredGroupEntry group = new DefaultGroup(
                        matchingExtraneousGroup.id(), groupDesc);
                // Insert the newly created group entry into key and id maps
                getGroupStoreKeyMap().
                        put(new GroupStoreKeyMapKey(groupDesc.deviceId(),
                                                    groupDesc.appCookie()), group);
                // Ensure it also inserted into group id based table to
                // avoid any chances of duplication in group id generation
                getGroupIdTable(groupDesc.deviceId()).
                        put(matchingExtraneousGroup.id(), group);
                addOrUpdateGroupEntry(matchingExtraneousGroup);
                removeExtraneousGroupEntry(matchingExtraneousGroup);
                return;
            } else {
                //TODO: Check if there are any empty groups that can be used here
                log.debug("storeGroupDescriptionInternal: No matching extraneous groups found in Device {}",
                          groupDesc.deviceId());
            }
        }

        GroupId id = null;
        if (groupDesc.givenGroupId() == null) {
            // Get a new group identifier
            id = new GroupId(getFreeGroupIdValue(groupDesc.deviceId()));
        } else {
            // we need to use the identifier passed in by caller, but check if
            // already used
            Group existing = getGroup(groupDesc.deviceId(),
                                      new GroupId(groupDesc.givenGroupId()));
            if (existing != null) {
                log.warn("Group already exists with the same id: 0x{} in dev:{} "
                                 + "but with different key: {} (request gkey: {})",
                         Integer.toHexString(groupDesc.givenGroupId()),
                         groupDesc.deviceId(),
                         existing.appCookie(),
                         groupDesc.appCookie());
                return;
            }
            id = new GroupId(groupDesc.givenGroupId());
        }
        // Create a group entry object
        StoredGroupEntry group = new DefaultGroup(id, groupDesc);
        // Insert the newly created group entry into key and id maps
        getGroupStoreKeyMap().
                put(new GroupStoreKeyMapKey(groupDesc.deviceId(),
                                            groupDesc.appCookie()), group);
        // Ensure it also inserted into group id based table to
        // avoid any chances of duplication in group id generation
        getGroupIdTable(groupDesc.deviceId()).
                put(id, group);
        log.debug("storeGroupDescriptionInternal: Processing Group ADD request for Id {} in device {}",
                  id,
                  groupDesc.deviceId());
        notifyDelegate(new GroupEvent(GroupEvent.Type.GROUP_ADD_REQUESTED,
                                      group));
    }

    /**
     * Updates the existing group entry with the information
     * from group description.
     *
     * @param deviceId     the device ID
     * @param oldAppCookie the current group key
     * @param type         update type
     * @param newBuckets   group buckets for updates
     * @param newAppCookie optional new group key
     */
    @Override
    public void updateGroupDescription(DeviceId deviceId,
                                       GroupKey oldAppCookie,
                                       UpdateType type,
                                       GroupBuckets newBuckets,
                                       GroupKey newAppCookie) {
        // Check if group update to be done by a remote instance
        if (mastershipService.getMasterFor(deviceId) != null &&
                mastershipService.getLocalRole(deviceId) != MastershipRole.MASTER) {
            log.debug("updateGroupDescription: Device {} local role is not MASTER",
                      deviceId);
            if (mastershipService.getMasterFor(deviceId) == null) {
                log.error("No Master for device {}..."
                                  + "Can not perform update group operation",
                          deviceId);
                //TODO: Send Group operation failure event
                return;
            }
            GroupStoreMessage groupOp = GroupStoreMessage.
                    createGroupUpdateRequestMsg(deviceId,
                                                oldAppCookie,
                                                type,
                                                newBuckets,
                                                newAppCookie);

            clusterCommunicator.unicast(groupOp,
                                        GroupStoreMessageSubjects.REMOTE_GROUP_OP_REQUEST,
                                        clusterMsgSerializer::serialize,
                                        mastershipService.getMasterFor(deviceId)).whenComplete((result, error) -> {
                if (error != null) {
                    log.warn("Failed to send request to master: {} to {}",
                             groupOp,
                             mastershipService.getMasterFor(deviceId), error);
                }
                //TODO: Send Group operation failure event
            });
            return;
        }
        log.debug("updateGroupDescription for device {} is getting handled locally",
                  deviceId);
        updateGroupDescriptionInternal(deviceId,
                                       oldAppCookie,
                                       type,
                                       newBuckets,
                                       newAppCookie);
    }

    private void updateGroupDescriptionInternal(DeviceId deviceId,
                                                GroupKey oldAppCookie,
                                                UpdateType type,
                                                GroupBuckets newBuckets,
                                                GroupKey newAppCookie) {
        // Check if a group is existing with the provided key
        Group oldGroup = getGroup(deviceId, oldAppCookie);
        if (oldGroup == null) {
            log.warn("updateGroupDescriptionInternal: Group not found...strange. "
                             + "GroupKey:{} DeviceId:{}", oldAppCookie, deviceId);
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
            log.debug("updateGroupDescriptionInternal: group entry {} in device {} moving from {} to PENDING_UPDATE",
                      oldGroup.id(),
                      oldGroup.deviceId(),
                      oldGroup.state());
            newGroup.setState(GroupState.PENDING_UPDATE);
            newGroup.setLife(oldGroup.life());
            newGroup.setPackets(oldGroup.packets());
            newGroup.setBytes(oldGroup.bytes());
            //Update the group entry in groupkey based map.
            //Update to groupid based map will happen in the
            //groupkey based map update listener
            log.debug("updateGroupDescriptionInternal with type {}: Group updated with buckets",
                      type);
            getGroupStoreKeyMap().
                    put(new GroupStoreKeyMapKey(newGroup.deviceId(),
                                                newGroup.appCookie()), newGroup);
            notifyDelegate(new GroupEvent(Type.GROUP_UPDATE_REQUESTED, newGroup));
        } else {
            log.warn("updateGroupDescriptionInternal with type {}: No "
                             + "change in the buckets in update", type);
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
     * @param deviceId  the device ID
     * @param appCookie the group key
     */
    @Override
    public void deleteGroupDescription(DeviceId deviceId,
                                       GroupKey appCookie) {
        // Check if group to be deleted by a remote instance
        if (mastershipService.
                getLocalRole(deviceId) != MastershipRole.MASTER) {
            log.debug("deleteGroupDescription: Device {} local role is not MASTER",
                      deviceId);
            if (mastershipService.getMasterFor(deviceId) == null) {
                log.error("No Master for device {}..."
                                  + "Can not perform delete group operation",
                          deviceId);
                //TODO: Send Group operation failure event
                return;
            }
            GroupStoreMessage groupOp = GroupStoreMessage.
                    createGroupDeleteRequestMsg(deviceId,
                                                appCookie);

            clusterCommunicator.unicast(groupOp,
                                        GroupStoreMessageSubjects.REMOTE_GROUP_OP_REQUEST,
                                        clusterMsgSerializer::serialize,
                                        mastershipService.getMasterFor(deviceId)).whenComplete((result, error) -> {
                if (error != null) {
                    log.warn("Failed to send request to master: {} to {}",
                             groupOp,
                             mastershipService.getMasterFor(deviceId), error);
                }
                //TODO: Send Group operation failure event
            });
            return;
        }
        log.debug("deleteGroupDescription in device {} is getting handled locally",
                  deviceId);
        deleteGroupDescriptionInternal(deviceId, appCookie);
    }

    private void deleteGroupDescriptionInternal(DeviceId deviceId,
                                                GroupKey appCookie) {
        // Check if a group is existing with the provided key
        StoredGroupEntry existing = getStoredGroupEntry(deviceId, appCookie);
        if (existing == null) {
            return;
        }

        log.debug("deleteGroupDescriptionInternal: group entry {} in device {} moving from {} to PENDING_DELETE",
                  existing.id(),
                  existing.deviceId(),
                  existing.state());
        synchronized (existing) {
            existing.setState(GroupState.PENDING_DELETE);
            getGroupStoreKeyMap().
                    put(new GroupStoreKeyMapKey(existing.deviceId(), existing.appCookie()),
                        existing);
        }
        log.debug("deleteGroupDescriptionInternal: in device {} issuing GROUP_REMOVE_REQUESTED",
                  deviceId);
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
        StoredGroupEntry existing = getStoredGroupEntry(group.deviceId(),
                                                        group.id());
        GroupEvent event = null;

        if (existing != null) {
            log.trace("addOrUpdateGroupEntry: updating group entry {} in device {}",
                      group.id(),
                      group.deviceId());
            synchronized (existing) {
                for (GroupBucket bucket : group.buckets().buckets()) {
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
                existing.setReferenceCount(group.referenceCount());
                if ((existing.state() == GroupState.PENDING_ADD) ||
                        (existing.state() == GroupState.PENDING_ADD_RETRY)) {
                    log.trace("addOrUpdateGroupEntry: group entry {} in device {} moving from {} to ADDED",
                              existing.id(),
                              existing.deviceId(),
                              existing.state());
                    existing.setState(GroupState.ADDED);
                    existing.setIsGroupStateAddedFirstTime(true);
                    event = new GroupEvent(Type.GROUP_ADDED, existing);
                } else {
                    log.trace("addOrUpdateGroupEntry: group entry {} in device {} moving from {} to ADDED",
                              existing.id(),
                              existing.deviceId(),
                              GroupState.PENDING_UPDATE);
                    existing.setState(GroupState.ADDED);
                    existing.setIsGroupStateAddedFirstTime(false);
                    event = new GroupEvent(Type.GROUP_UPDATED, existing);
                }
                //Re-PUT map entries to trigger map update events
                getGroupStoreKeyMap().
                        put(new GroupStoreKeyMapKey(existing.deviceId(),
                                                    existing.appCookie()), existing);
            }
        } else {
            log.warn("addOrUpdateGroupEntry: Group update "
                             + "happening for a non-existing entry in the map");
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
        StoredGroupEntry existing = getStoredGroupEntry(group.deviceId(),
                                                        group.id());

        if (existing != null) {
            log.debug("removeGroupEntry: removing group entry {} in device {}",
                      group.id(),
                      group.deviceId());
            //Removal from groupid based map will happen in the
            //map update listener
            getGroupStoreKeyMap().remove(new GroupStoreKeyMapKey(existing.deviceId(),
                                                                 existing.appCookie()));
            notifyDelegate(new GroupEvent(Type.GROUP_REMOVED, existing));
        } else {
            log.warn("removeGroupEntry for {} in device{} is "
                             + "not existing in our maps",
                     group.id(),
                     group.deviceId());
        }
    }

    private void purgeGroupEntries(Set<Entry<GroupStoreKeyMapKey, StoredGroupEntry>> entries) {
        entries.forEach(entry -> {
            groupStoreEntriesByKey.remove(entry.getKey());
        });
    }

    @Override
    public void purgeGroupEntry(DeviceId deviceId) {
        Set<Entry<GroupStoreKeyMapKey, StoredGroupEntry>> entriesPendingRemove =
                new HashSet<>();

        getGroupStoreKeyMap().entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId))
                .forEach(entriesPendingRemove::add);

        purgeGroupEntries(entriesPendingRemove);
    }

    @Override
    public void purgeGroupEntries() {
        purgeGroupEntries(getGroupStoreKeyMap().entrySet());
    }

    @Override
    public void deviceInitialAuditCompleted(DeviceId deviceId,
                                            boolean completed) {
        synchronized (deviceAuditStatus) {
            if (completed) {
                log.debug("AUDIT completed for device {}",
                          deviceId);
                deviceAuditStatus.put(deviceId, true);
                // Execute all pending group requests
                List<StoredGroupEntry> pendingGroupRequests =
                        getPendingGroupKeyTable().values()
                                .stream()
                                .filter(g -> g.deviceId().equals(deviceId))
                                .collect(Collectors.toList());
                log.debug("processing pending group add requests for device {} and number of pending requests {}",
                          deviceId,
                          pendingGroupRequests.size());
                for (Group group : pendingGroupRequests) {
                    GroupDescription tmp = new DefaultGroupDescription(
                            group.deviceId(),
                            group.type(),
                            group.buckets(),
                            group.appCookie(),
                            group.givenGroupId(),
                            group.appId());
                    storeGroupDescriptionInternal(tmp);
                    getPendingGroupKeyTable().
                            remove(new GroupStoreKeyMapKey(deviceId, group.appCookie()));
                }
            } else {
                Boolean audited = deviceAuditStatus.get(deviceId);
                if (audited != null && audited) {
                    log.debug("Clearing AUDIT status for device {}", deviceId);
                    deviceAuditStatus.put(deviceId, false);
                }
            }
        }
    }

    @Override
    public boolean deviceInitialAuditStatus(DeviceId deviceId) {
        synchronized (deviceAuditStatus) {
            Boolean audited = deviceAuditStatus.get(deviceId);
            return audited != null && audited;
        }
    }

    @Override
    public void groupOperationFailed(DeviceId deviceId, GroupOperation operation) {

        StoredGroupEntry existing = getStoredGroupEntry(deviceId,
                                                        operation.groupId());

        if (existing == null) {
            log.warn("No group entry with ID {} found ", operation.groupId());
            return;
        }

        log.warn("groupOperationFailed: group operation {} failed"
                         + "for group {} in device {} with code {}",
                 operation.opType(),
                 existing.id(),
                 existing.deviceId(),
                 operation.failureCode());
        if (operation.failureCode() == GroupOperation.GroupMsgErrorCode.GROUP_EXISTS) {
            if (operation.buckets().equals(existing.buckets())) {
                if (existing.state() == GroupState.PENDING_ADD ||
                        existing.state() == GroupState.PENDING_ADD_RETRY) {
                    log.info("GROUP_EXISTS: GroupID and Buckets match for group in pending "
                                     + "add state - moving to ADDED for group {} in device {}",
                             existing.id(), deviceId);
                    addOrUpdateGroupEntry(existing);
                    return;
                } else {
                    log.warn("GROUP_EXISTS: GroupId and Buckets match but existing"
                            + "group in state: {}", existing.state());
                }
            } else {
                log.warn("GROUP EXISTS: Group ID matched but buckets did not. "
                        + "Operation: {} Existing: {}", operation.buckets(),
                        existing.buckets());
            }
        }
        switch (operation.opType()) {
            case ADD:
                if (existing.state() == GroupState.PENDING_ADD) {
                    notifyDelegate(new GroupEvent(Type.GROUP_ADD_FAILED, existing));
                    log.warn("groupOperationFailed: cleaningup "
                                     + "group {} from store in device {}....",
                             existing.id(),
                             existing.deviceId());
                    //Removal from groupid based map will happen in the
                    //map update listener
                    getGroupStoreKeyMap().remove(new GroupStoreKeyMapKey(existing.deviceId(),
                                                                         existing.appCookie()));
                }
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
    }

    @Override
    public void addOrUpdateExtraneousGroupEntry(Group group) {
        log.debug("add/update extraneous group entry {} in device {}",
                  group.id(),
                  group.deviceId());
        ConcurrentMap<GroupId, Group> extraneousIdTable =
                getExtraneousGroupIdTable(group.deviceId());
        extraneousIdTable.put(group.id(), group);
        // Don't remove the extraneous groups, instead re-use it when
        // a group request comes with the same set of buckets
    }

    @Override
    public void removeExtraneousGroupEntry(Group group) {
        log.debug("remove extraneous group entry {} of device {} from store",
                  group.id(),
                  group.deviceId());
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

    /**
     * Map handler to receive any events when the group key map is updated.
     */
    private class GroupStoreKeyMapListener implements
            MapEventListener<GroupStoreKeyMapKey, StoredGroupEntry> {

        @Override
        public void event(MapEvent<GroupStoreKeyMapKey, StoredGroupEntry> mapEvent) {
            GroupEvent groupEvent = null;
            GroupStoreKeyMapKey key = mapEvent.key();
            StoredGroupEntry group = Versioned.valueOrNull(mapEvent.newValue());
            if ((key == null) && (group == null)) {
                log.error("GroupStoreKeyMapListener: Received "
                                  + "event {} with null entry", mapEvent.type());
                return;
            } else if (group == null) {
                group = getGroupIdTable(key.deviceId()).values()
                        .stream()
                        .filter((storedGroup) -> (storedGroup.appCookie().equals(key.appCookie)))
                        .findFirst().orElse(null);
                if (group == null) {
                    log.error("GroupStoreKeyMapListener: Received "
                                      + "event {} with null entry... can not process", mapEvent.type());
                    return;
                }
            }
            log.trace("received groupid map event {} for id {} in device {}",
                      mapEvent.type(),
                      group.id(),
                      (key != null ? key.deviceId() : null));
            if (mapEvent.type() == MapEvent.Type.INSERT || mapEvent.type() == MapEvent.Type.UPDATE) {
                // Update the group ID table
                getGroupIdTable(group.deviceId()).put(group.id(), group);
                StoredGroupEntry value = Versioned.valueOrNull(mapEvent.newValue());
                if (value.state() == Group.GroupState.ADDED) {
                    if (value.isGroupStateAddedFirstTime()) {
                        groupEvent = new GroupEvent(Type.GROUP_ADDED, value);
                        log.trace("Received first time GROUP_ADDED state update for id {} in device {}",
                                  group.id(),
                                  group.deviceId());
                    } else {
                        groupEvent = new GroupEvent(Type.GROUP_UPDATED, value);
                        log.trace("Received following GROUP_ADDED state update for id {} in device {}",
                                  group.id(),
                                  group.deviceId());
                    }
                }
            } else if (mapEvent.type() == MapEvent.Type.REMOVE) {
                groupEvent = new GroupEvent(Type.GROUP_REMOVED, group);
                // Remove the entry from the group ID table
                getGroupIdTable(group.deviceId()).remove(group.id(), group);
            }

            if (groupEvent != null) {
                notifyDelegate(groupEvent);
            }
        }
    }

    private void processGroupMessage(GroupStoreMessage message) {
        if (message.type() == GroupStoreMessage.Type.FAILOVER) {
            // FIXME: groupStoreEntriesByKey inaccessible here
            getGroupIdTable(message.deviceId()).values()
                    .stream()
                    .filter((storedGroup) -> (storedGroup.appCookie().equals(message.appCookie())))
                    .findFirst().ifPresent(group -> notifyDelegate(new GroupEvent(Type.GROUP_BUCKET_FAILOVER, group)));
        }
    }

    private void process(GroupStoreMessage groupOp) {
        log.debug("Received remote group operation {} request for device {}",
                  groupOp.type(),
                  groupOp.deviceId());
        if (!mastershipService.isLocalMaster(groupOp.deviceId())) {
            log.warn("This node is not MASTER for device {}", groupOp.deviceId());
            return;
        }
        if (groupOp.type() == GroupStoreMessage.Type.ADD) {
            storeGroupDescriptionInternal(groupOp.groupDesc());
        } else if (groupOp.type() == GroupStoreMessage.Type.UPDATE) {
            updateGroupDescriptionInternal(groupOp.deviceId(),
                                           groupOp.appCookie(),
                                           groupOp.updateType(),
                                           groupOp.updateBuckets(),
                                           groupOp.newAppCookie());
        } else if (groupOp.type() == GroupStoreMessage.Type.DELETE) {
            deleteGroupDescriptionInternal(groupOp.deviceId(),
                                           groupOp.appCookie());
        }
    }

    /**
     * Flattened map key to be used to store group entries.
     */
    protected static class GroupStoreMapKey {
        private final DeviceId deviceId;

        public GroupStoreMapKey(DeviceId deviceId) {
            this.deviceId = deviceId;
        }

        public DeviceId deviceId() {
            return deviceId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GroupStoreMapKey)) {
                return false;
            }
            GroupStoreMapKey that = (GroupStoreMapKey) o;
            return this.deviceId.equals(that.deviceId);
        }

        @Override
        public int hashCode() {
            int result = 17;

            result = 31 * result + Objects.hash(this.deviceId);

            return result;
        }
    }

    protected static class GroupStoreKeyMapKey extends GroupStoreMapKey {
        private final GroupKey appCookie;

        public GroupStoreKeyMapKey(DeviceId deviceId,
                                   GroupKey appCookie) {
            super(deviceId);
            this.appCookie = appCookie;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GroupStoreKeyMapKey)) {
                return false;
            }
            GroupStoreKeyMapKey that = (GroupStoreKeyMapKey) o;
            return (super.equals(that) &&
                    this.appCookie.equals(that.appCookie));
        }

        @Override
        public int hashCode() {
            int result = 17;

            result = 31 * result + super.hashCode() + Objects.hash(this.appCookie);

            return result;
        }
    }

    protected static class GroupStoreIdMapKey extends GroupStoreMapKey {
        private final GroupId groupId;

        public GroupStoreIdMapKey(DeviceId deviceId,
                                  GroupId groupId) {
            super(deviceId);
            this.groupId = groupId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GroupStoreIdMapKey)) {
                return false;
            }
            GroupStoreIdMapKey that = (GroupStoreIdMapKey) o;
            return (super.equals(that) &&
                    this.groupId.equals(that.groupId));
        }

        @Override
        public int hashCode() {
            int result = 17;

            result = 31 * result + super.hashCode() + Objects.hash(this.groupId);

            return result;
        }
    }

    @Override
    public void pushGroupMetrics(DeviceId deviceId,
                                 Collection<Group> groupEntries) {
        boolean deviceInitialAuditStatus =
                deviceInitialAuditStatus(deviceId);
        Set<Group> southboundGroupEntries =
                Sets.newHashSet(groupEntries);
        Set<StoredGroupEntry> storedGroupEntries =
                Sets.newHashSet(getStoredGroups(deviceId));
        Set<Group> extraneousStoredEntries =
                Sets.newHashSet(getExtraneousGroups(deviceId));

        if (log.isTraceEnabled()) {
            log.trace("pushGroupMetrics: Displaying all ({}) southboundGroupEntries for device {}",
                    southboundGroupEntries.size(),
                    deviceId);
            for (Group group : southboundGroupEntries) {
                log.trace("Group {} in device {}", group, deviceId);
            }

            log.trace("Displaying all ({}) stored group entries for device {}",
                    storedGroupEntries.size(),
                    deviceId);
            for (StoredGroupEntry group : storedGroupEntries) {
                log.trace("Stored Group {} for device {}", group, deviceId);
            }
        }

        garbageCollect(deviceId, southboundGroupEntries, storedGroupEntries);

        for (Iterator<Group> it2 = southboundGroupEntries.iterator(); it2.hasNext();) {
            Group group = it2.next();
            if (storedGroupEntries.remove(group)) {
                // we both have the group, let's update some info then.
                log.trace("Group AUDIT: group {} exists in both planes for device {}",
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
                log.debug("Group AUDIT: extraneous group {} exists in data plane for device {}",
                          group.id(), deviceId);
                extraneousStoredEntries.remove(group);
                if (allowExtraneousGroups) {
                    extraneousGroup(group);
                } else {
                    notifyDelegate(new GroupEvent(Type.GROUP_REMOVE_REQUESTED, group));
                }
            }
        }
        for (Group group : storedGroupEntries) {
            // there are groups in the store that aren't in the switch
            log.debug("Group AUDIT: group {} missing in data plane for device {}",
                      group.id(), deviceId);
            groupMissing(group);
        }
        for (Group group : extraneousStoredEntries) {
            // there are groups in the extraneous store that
            // aren't in the switch
            log.debug("Group AUDIT: clearing extraneous group {} from store for device {}",
                      group.id(), deviceId);
            removeExtraneousGroupEntry(group);
        }

        if (!deviceInitialAuditStatus) {
            log.info("Group AUDIT: Setting device {} initial AUDIT completed",
                     deviceId);
            deviceInitialAuditCompleted(deviceId, true);
        }
    }

    @Override
    public void notifyOfFailovers(Collection<Group> failoverGroups) {
        failoverGroups.forEach(group -> {
            if (group.type() == Group.Type.FAILOVER) {
                groupTopic.publish(GroupStoreMessage.createGroupFailoverMsg(
                        group.deviceId(), group));
            }
        });
    }

    private void garbageCollect(DeviceId deviceId,
                                Set<Group> southboundGroupEntries,
                                Set<StoredGroupEntry> storedGroupEntries) {
        if (!garbageCollect) {
            return;
        }

        Iterator<StoredGroupEntry> it = storedGroupEntries.iterator();
        while (it.hasNext()) {
            StoredGroupEntry group = it.next();
            if (group.state() != GroupState.PENDING_DELETE && checkGroupRefCount(group)) {
                log.debug("Garbage collecting group {} on {}", group, deviceId);
                deleteGroupDescription(deviceId, group.appCookie());
                southboundGroupEntries.remove(group);
                it.remove();
            }
        }
    }

    private boolean checkGroupRefCount(Group group) {
        return (group.referenceCount() == 0 && group.age() >= gcThresh);
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
            case PENDING_ADD_RETRY:
            case PENDING_UPDATE:
                log.debug("Group {} is in store but not on device {}",
                          group, group.deviceId());
                StoredGroupEntry existing =
                        getStoredGroupEntry(group.deviceId(), group.id());
                log.debug("groupMissing: group entry {} in device {} moving from {} to PENDING_ADD_RETRY",
                          existing.id(),
                          existing.deviceId(),
                          existing.state());
                existing.setState(Group.GroupState.PENDING_ADD_RETRY);
                //Re-PUT map entries to trigger map update events
                getGroupStoreKeyMap().
                        put(new GroupStoreKeyMapKey(existing.deviceId(),
                                                    existing.appCookie()), existing);
                notifyDelegate(new GroupEvent(GroupEvent.Type.GROUP_ADD_REQUESTED,
                                              group));
                break;
            default:
                log.debug("Group {} has not been installed.", group);
                break;
        }
    }

    private void extraneousGroup(Group group) {
        log.trace("Group {} is on device {} but not in store.",
                  group, group.deviceId());
        addOrUpdateExtraneousGroupEntry(group);
    }

    private void groupAdded(Group group) {
        log.trace("Group {} Added or Updated in device {}",
                  group, group.deviceId());
        addOrUpdateGroupEntry(group);
    }
}
