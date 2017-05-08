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

package org.onosproject.incubator.store.virtual.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkMastershipStore;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipStoreDelegate;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the virtual network mastership store to manage inventory of
 * mastership using trivial in-memory implementation.
 */
@Component(immediate = true)
@Service
public class SimpleVirtualMastershipStore
        extends AbstractVirtualStore<MastershipEvent, MastershipStoreDelegate>
        implements VirtualNetworkMastershipStore {

    private final Logger log = getLogger(getClass());

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public CompletableFuture<MastershipRole> requestRole(NetworkId networkId, DeviceId deviceId) {
        return null;
    }

    @Override
    public MastershipRole getRole(NetworkId networkId, NodeId nodeId, DeviceId deviceId) {
        return null;
    }

    @Override
    public NodeId getMaster(NetworkId networkId, DeviceId deviceId) {
        return null;
    }

    @Override
    public RoleInfo getNodes(NetworkId networkId, DeviceId deviceId) {
        return null;
    }

    @Override
    public Set<DeviceId> getDevices(NetworkId networkId, NodeId nodeId) {
        return null;
    }

    @Override
    public CompletableFuture<MastershipEvent> setMaster(NetworkId networkId,
                                                        NodeId nodeId, DeviceId deviceId) {
        return null;
    }

    @Override
    public MastershipTerm getTermFor(NetworkId networkId, DeviceId deviceId) {
        return null;
    }

    @Override
    public CompletableFuture<MastershipEvent> setStandby(NetworkId networkId,
                                                         NodeId nodeId, DeviceId deviceId) {
        return null;
    }

    @Override
    public CompletableFuture<MastershipEvent> relinquishRole(NetworkId networkId,
                                                             NodeId nodeId, DeviceId deviceId) {
        return null;
    }

    @Override
    public void relinquishAllRole(NetworkId networkId, NodeId nodeId) {

    }
}
