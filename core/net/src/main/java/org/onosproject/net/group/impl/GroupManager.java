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
package org.onosproject.net.group.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupListener;
import org.onosproject.net.group.GroupOperation;
import org.onosproject.net.group.GroupOperations;
import org.onosproject.net.group.GroupProvider;
import org.onosproject.net.group.GroupProviderRegistry;
import org.onosproject.net.group.GroupProviderService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.group.GroupStore;
import org.onosproject.net.group.GroupStore.UpdateType;
import org.onosproject.net.group.GroupStoreDelegate;
import org.onosproject.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.security.AppPermission.Type.*;



/**
 * Provides implementation of the group service APIs.
 */
@Component(immediate = true)
@Service
public class GroupManager
        extends AbstractListenerProviderRegistry<GroupEvent, GroupListener,
        GroupProvider, GroupProviderService>
        implements GroupService, GroupProviderRegistry {

    private final Logger log = getLogger(getClass());

    private final GroupStoreDelegate delegate = new InternalGroupStoreDelegate();
    private final DeviceListener deviceListener = new InternalDeviceListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(GroupEvent.class, listenerRegistry);
        deviceService.addListener(deviceListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(GroupEvent.class);
        log.info("Stopped");
    }

    /**
     * Create a group in the specified device with the provided parameters.
     *
     * @param groupDesc group creation parameters
     */
    @Override
    public void addGroup(GroupDescription groupDesc) {
        checkPermission(GROUP_WRITE);
        store.storeGroupDescription(groupDesc);
    }

    /**
     * Return a group object associated to an application cookie.
     * <p>
     * NOTE1: The presence of group object in the system does not
     * guarantee that the "group" is actually created in device.
     * GROUP_ADDED notification would confirm the creation of
     * this group in data plane.
     *
     * @param deviceId  device identifier
     * @param appCookie application cookie to be used for lookup
     * @return group associated with the application cookie or
     * NULL if Group is not found for the provided cookie
     */
    @Override
    public Group getGroup(DeviceId deviceId, GroupKey appCookie) {
        checkPermission(GROUP_READ);
        return store.getGroup(deviceId, appCookie);
    }

    /**
     * Append buckets to existing group. The caller can optionally
     * associate a new cookie during this updation. GROUP_UPDATED or
     * GROUP_UPDATE_FAILED notifications would be provided along with
     * cookie depending on the result of the operation on the device.
     *
     * @param deviceId  device identifier
     * @param oldCookie cookie to be used to retrieve the existing group
     * @param buckets   immutable list of group bucket to be added
     * @param newCookie immutable cookie to be used post update operation
     * @param appId     Application Id
     */
    @Override
    public void addBucketsToGroup(DeviceId deviceId,
                                  GroupKey oldCookie,
                                  GroupBuckets buckets,
                                  GroupKey newCookie,
                                  ApplicationId appId) {
        checkPermission(GROUP_WRITE);
        store.updateGroupDescription(deviceId,
                                     oldCookie,
                                     UpdateType.ADD,
                                     buckets,
                                     newCookie);
    }

    /**
     * Remove buckets from existing group. The caller can optionally
     * associate a new cookie during this updation. GROUP_UPDATED or
     * GROUP_UPDATE_FAILED notifications would be provided along with
     * cookie depending on the result of the operation on the device.
     *
     * @param deviceId  device identifier
     * @param oldCookie cookie to be used to retrieve the existing group
     * @param buckets   immutable list of group bucket to be removed
     * @param newCookie immutable cookie to be used post update operation
     * @param appId     Application Id
     */
    @Override
    public void removeBucketsFromGroup(DeviceId deviceId,
                                       GroupKey oldCookie,
                                       GroupBuckets buckets,
                                       GroupKey newCookie,
                                       ApplicationId appId) {
        checkPermission(GROUP_WRITE);
        store.updateGroupDescription(deviceId,
                                     oldCookie,
                                     UpdateType.REMOVE,
                                     buckets,
                                     newCookie);
    }

    /**
     * Delete a group associated to an application cookie.
     * GROUP_DELETED or GROUP_DELETE_FAILED notifications would be
     * provided along with cookie depending on the result of the
     * operation on the device.
     *
     * @param deviceId  device identifier
     * @param appCookie application cookie to be used for lookup
     * @param appId     Application Id
     */
    @Override
    public void removeGroup(DeviceId deviceId,
                            GroupKey appCookie,
                            ApplicationId appId) {
        checkPermission(GROUP_WRITE);
        store.deleteGroupDescription(deviceId, appCookie);
    }

    /**
     * Retrieve all groups created by an application in the specified device
     * as seen by current controller instance.
     *
     * @param deviceId device identifier
     * @param appId    application id
     * @return collection of immutable group objects created by the application
     */
    @Override
    public Iterable<Group> getGroups(DeviceId deviceId,
                                     ApplicationId appId) {
        checkPermission(GROUP_READ);
        return store.getGroups(deviceId);
    }

    @Override
    public Iterable<Group> getGroups(DeviceId deviceId) {
        checkPermission(GROUP_READ);
        return store.getGroups(deviceId);
    }

    @Override
    protected GroupProviderService createProviderService(GroupProvider provider) {
        return new InternalGroupProviderService(provider);
    }

    private class InternalGroupStoreDelegate implements GroupStoreDelegate {
        @Override
        public void notify(GroupEvent event) {
            final Group group = event.subject();
            GroupProvider groupProvider =
                    getProvider(group.deviceId());
            GroupOperations groupOps = null;
            switch (event.type()) {
                case GROUP_ADD_REQUESTED:
                    log.debug("GROUP_ADD_REQUESTED for Group {} on device {}",
                              group.id(), group.deviceId());
                    GroupOperation groupAddOp = GroupOperation.
                            createAddGroupOperation(group.id(),
                                                    group.type(),
                                                    group.buckets());
                    groupOps = new GroupOperations(
                            Collections.singletonList(groupAddOp));
                    groupProvider.performGroupOperation(group.deviceId(), groupOps);
                    break;

                case GROUP_UPDATE_REQUESTED:
                    log.debug("GROUP_UPDATE_REQUESTED for Group {} on device {}",
                              group.id(), group.deviceId());
                    GroupOperation groupModifyOp = GroupOperation.
                            createModifyGroupOperation(group.id(),
                                                       group.type(),
                                                       group.buckets());
                    groupOps = new GroupOperations(
                            Collections.singletonList(groupModifyOp));
                    groupProvider.performGroupOperation(group.deviceId(), groupOps);
                    break;

                case GROUP_REMOVE_REQUESTED:
                    log.debug("GROUP_REMOVE_REQUESTED for Group {} on device {}",
                              group.id(), group.deviceId());
                    GroupOperation groupDeleteOp = GroupOperation.
                            createDeleteGroupOperation(group.id(),
                                                       group.type());
                    groupOps = new GroupOperations(
                            Collections.singletonList(groupDeleteOp));
                    groupProvider.performGroupOperation(group.deviceId(), groupOps);
                    break;

                case GROUP_ADDED:
                case GROUP_UPDATED:
                case GROUP_REMOVED:
                case GROUP_ADD_FAILED:
                case GROUP_UPDATE_FAILED:
                case GROUP_REMOVE_FAILED:
                    post(event);
                    break;

                default:
                    break;
            }
        }
    }

    private class InternalGroupProviderService
            extends AbstractProviderService<GroupProvider>
            implements GroupProviderService {

        protected InternalGroupProviderService(GroupProvider provider) {
            super(provider);
        }

        @Override
        public void groupOperationFailed(DeviceId deviceId, GroupOperation operation) {
            store.groupOperationFailed(deviceId, operation);
        }

        @Override
        public void pushGroupMetrics(DeviceId deviceId,
                                     Collection<Group> groupEntries) {
            log.trace("Received group metrics from device {}", deviceId);
            checkValidity();
            store.pushGroupMetrics(deviceId, groupEntries);
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_REMOVED:
                case DEVICE_AVAILABILITY_CHANGED:
                    if (!deviceService.isAvailable(event.subject().id())) {
                        log.debug("Device {} became un available; clearing initial audit status",
                                  event.type(), event.subject().id());
                        store.deviceInitialAuditCompleted(event.subject().id(), false);
                    }
                    break;

                default:
                    break;
            }
        }
    }
}
