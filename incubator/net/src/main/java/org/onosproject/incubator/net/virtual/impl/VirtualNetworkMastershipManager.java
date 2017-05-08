/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.onlab.metrics.MetricsService;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.core.MetricsHelper;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkMastershipStore;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.event.AbstractVirtualListenerManager;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipStoreDelegate;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.mastership.MastershipTermService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class VirtualNetworkMastershipManager
        extends AbstractVirtualListenerManager<MastershipEvent, MastershipListener>
        implements MastershipService, MastershipAdminService, MastershipTermService,
        MetricsHelper {

    VirtualNetworkMastershipStore store;
    MastershipStoreDelegate storeDelegate;

    /**
     * Creates a new VirtualNetworkMastershipManager object.
     *
     * @param manager virtual network manager service
     * @param networkId virtual network identifier
     */
    public VirtualNetworkMastershipManager(VirtualNetworkService manager, NetworkId networkId) {
        super(manager, networkId, MastershipEvent.class);

        store = serviceDirectory.get(VirtualNetworkMastershipStore.class);
        this.storeDelegate = new InternalDelegate();
        store.setDelegate(networkId, this.storeDelegate);
    }

    @Override
    public MetricsService metricsService() {
        return null;
    }

    @Override
    public MastershipTerm getMastershipTerm(DeviceId deviceId) {
        return null;
    }

    @Override
    public CompletableFuture<Void> setRole(NodeId instance, DeviceId deviceId, MastershipRole role) {
        return null;
    }

    @Override
    public void balanceRoles() {

    }

    @Override
    public MastershipRole getLocalRole(DeviceId deviceId) {
        return null;
    }

    @Override
    public CompletableFuture<MastershipRole> requestRoleFor(DeviceId deviceId) {
        return null;
    }

    @Override
    public CompletableFuture<Void> relinquishMastership(DeviceId deviceId) {
        return null;
    }

    @Override
    public NodeId getMasterFor(DeviceId deviceId) {
        return null;
    }

    @Override
    public RoleInfo getNodesFor(DeviceId deviceId) {
        return null;
    }

    @Override
    public Set<DeviceId> getDevicesOf(NodeId nodeId) {
        return null;
    }

    public class InternalDelegate implements MastershipStoreDelegate {
        @Override
        public void notify(MastershipEvent event) {
            post(event);
        }
    }
}
