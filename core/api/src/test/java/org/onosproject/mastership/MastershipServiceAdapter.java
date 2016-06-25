/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.mastership;

import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Test adapter for mastership service.
 */
public class MastershipServiceAdapter implements MastershipService {
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
    public Set<DeviceId> getDevicesOf(NodeId nodeId) {
        return null;
    }

    @Override
    public void addListener(MastershipListener listener) {
    }

    @Override
    public void removeListener(MastershipListener listener) {
    }

    @Override
    public RoleInfo getNodesFor(DeviceId deviceId) {
        return null;
    }
}
