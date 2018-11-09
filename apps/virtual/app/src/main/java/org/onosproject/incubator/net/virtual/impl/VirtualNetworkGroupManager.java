/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkGroupStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.event.AbstractVirtualListenerManager;
import org.onosproject.incubator.net.virtual.provider.AbstractVirtualProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualGroupProvider;
import org.onosproject.incubator.net.virtual.provider.VirtualGroupProviderService;
import org.onosproject.incubator.net.virtual.provider.VirtualProviderRegistryService;
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
import org.onosproject.net.group.GroupService;
import org.onosproject.net.group.GroupStoreDelegate;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Group service implementation built on the virtual network service.
 */
public class VirtualNetworkGroupManager
        extends AbstractVirtualListenerManager<GroupEvent, GroupListener>
        implements GroupService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final VirtualNetworkGroupStore store;

    private VirtualProviderRegistryService providerRegistryService = null;
    private VirtualGroupProviderService innerProviderService;
    private InternalStoreDelegate storeDelegate;
    private DeviceService deviceService;

    //TODO: make this configurable
    private boolean purgeOnDisconnection = false;

    public VirtualNetworkGroupManager(VirtualNetworkService manager, NetworkId networkId) {
        super(manager, networkId, GroupEvent.class);

        store = serviceDirectory.get(VirtualNetworkGroupStore.class);
        deviceService = manager.get(networkId, DeviceService.class);

        providerRegistryService =
                serviceDirectory.get(VirtualProviderRegistryService.class);
        innerProviderService = new InternalGroupProviderService();
        providerRegistryService.registerProviderService(networkId(), innerProviderService);

        this.storeDelegate = new InternalStoreDelegate();
        store.setDelegate(networkId, this.storeDelegate);

        log.info("Started");
    }

    @Override
    public void addGroup(GroupDescription groupDesc) {
        store.storeGroupDescription(networkId(), groupDesc);
    }

    @Override
    public Group getGroup(DeviceId deviceId, GroupKey appCookie) {
        return store.getGroup(networkId(), deviceId, appCookie);
    }

    @Override
    public void addBucketsToGroup(DeviceId deviceId, GroupKey oldCookie, GroupBuckets buckets,
                                  GroupKey newCookie, ApplicationId appId) {
        store.updateGroupDescription(networkId(),
                                     deviceId,
                                     oldCookie,
                                     VirtualNetworkGroupStore.UpdateType.ADD,
                                     buckets,
                                     newCookie);
    }

    @Override
    public void removeBucketsFromGroup(DeviceId deviceId, GroupKey oldCookie,
                                       GroupBuckets buckets, GroupKey newCookie,
                                       ApplicationId appId) {
        store.updateGroupDescription(networkId(),
                                     deviceId,
                                     oldCookie,
                                     VirtualNetworkGroupStore.UpdateType.REMOVE,
                                     buckets,
                                     newCookie);

    }

    @Override
    public void setBucketsForGroup(DeviceId deviceId,
                                   GroupKey oldCookie,
                                   GroupBuckets buckets,
                                   GroupKey newCookie,
                                   ApplicationId appId) {
        store.updateGroupDescription(networkId(),
                                     deviceId,
                                     oldCookie,
                                     VirtualNetworkGroupStore.UpdateType.SET,
                                     buckets,
                                     newCookie);
    }

    @Override
    public void purgeGroupEntries(DeviceId deviceId) {
        store.purgeGroupEntry(networkId(), deviceId);
    }

    @Override
    public void purgeGroupEntries() {
        store.purgeGroupEntries(networkId());
    }

    @Override
    public void removeGroup(DeviceId deviceId, GroupKey appCookie, ApplicationId appId) {
        store.deleteGroupDescription(networkId(), deviceId, appCookie);
    }

    @Override
    public Iterable<Group> getGroups(DeviceId deviceId, ApplicationId appId) {
        return store.getGroups(networkId(), deviceId);
    }

    @Override
    public Iterable<Group> getGroups(DeviceId deviceId) {
        return store.getGroups(networkId(), deviceId);
    }

    private class InternalGroupProviderService
            extends AbstractVirtualProviderService<VirtualGroupProvider>
            implements VirtualGroupProviderService {

        protected InternalGroupProviderService() {
            Set<ProviderId> providerIds =
                    providerRegistryService.getProvidersByService(this);
            ProviderId providerId = providerIds.stream().findFirst().get();
            VirtualGroupProvider provider = (VirtualGroupProvider)
                    providerRegistryService.getProvider(providerId);
            setProvider(provider);
        }

        @Override
        public void groupOperationFailed(DeviceId deviceId,
                                         GroupOperation operation) {
            store.groupOperationFailed(networkId(), deviceId, operation);
        }

        @Override
        public void pushGroupMetrics(DeviceId deviceId, Collection<Group> groupEntries) {
            log.trace("Received group metrics from device {}", deviceId);
            checkValidity();
            store.pushGroupMetrics(networkId(), deviceId, groupEntries);
        }

        @Override
        public void notifyOfFailovers(Collection<Group> failoverGroups) {
            store.notifyOfFailovers(networkId(), failoverGroups);
        }
    }

    private class InternalStoreDelegate implements GroupStoreDelegate {
        @Override
        public void notify(GroupEvent event) {
            final Group group = event.subject();
            VirtualGroupProvider groupProvider = innerProviderService.provider();
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
                    groupProvider.performGroupOperation(networkId(), group.deviceId(),
                                                        groupOps);
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
                    groupProvider.performGroupOperation(networkId(), group.deviceId(),
                                                        groupOps);
                    break;

                case GROUP_REMOVE_REQUESTED:
                    log.debug("GROUP_REMOVE_REQUESTED for Group {} on device {}",
                              group.id(), group.deviceId());
                    GroupOperation groupDeleteOp = GroupOperation.
                            createDeleteGroupOperation(group.id(),
                                                       group.type());
                    groupOps = new GroupOperations(
                            Collections.singletonList(groupDeleteOp));
                    groupProvider.performGroupOperation(networkId(), group.deviceId(),
                                                        groupOps);
                    break;

                case GROUP_ADDED:
                case GROUP_UPDATED:
                case GROUP_REMOVED:
                case GROUP_ADD_FAILED:
                case GROUP_UPDATE_FAILED:
                case GROUP_REMOVE_FAILED:
                case GROUP_BUCKET_FAILOVER:
                    post(event);
                    break;
                default:
                    break;
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_REMOVED:
                case DEVICE_AVAILABILITY_CHANGED:
                    DeviceId deviceId = event.subject().id();
                    if (!deviceService.isAvailable(deviceId)) {
                        log.debug("Device {} became un available; clearing initial audit status",
                                  event.type(), event.subject().id());
                        store.deviceInitialAuditCompleted(networkId(), event.subject().id(), false);

                        if (purgeOnDisconnection) {
                            store.purgeGroupEntry(networkId(), deviceId);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
