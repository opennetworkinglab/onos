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
package org.onosproject.drivers.netconf;

import org.onosproject.mastership.MastershipServiceAdapter;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.mastership.MastershipInfo;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;

public class MockMastershipService extends MastershipServiceAdapter {

    public MockMastershipService() {
    }

    @Override
    public boolean isLocalMaster(DeviceId deviceId) {
        if (deviceId != null && deviceId.uri().toString().equalsIgnoreCase("netconf:1.2.3.4:830")) {
            return true;
        }
        return false;
    }

    @Override
    public void addListener(MastershipListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeListener(MastershipListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public MastershipRole getLocalRole(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<MastershipRole> requestRoleFor(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<Void> relinquishMastership(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NodeId getMasterFor(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RoleInfo getNodesFor(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MastershipInfo getMastershipFor(DeviceId deviceId) {
        return null;
    }

    @Override
    public Set<DeviceId> getDevicesOf(NodeId nodeId) {
        // TODO Auto-generated method stub
        return null;
    }
}
