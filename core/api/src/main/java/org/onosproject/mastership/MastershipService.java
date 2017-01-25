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

import static org.onosproject.net.MastershipRole.MASTER;
import static org.onosproject.net.MastershipRole.NONE;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.onlab.util.Tools;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;

/**
 * Service responsible for determining the controller instance mastership of
 * a device in a clustered environment. This is the central authority for
 * determining mastership, but is not responsible for actually applying it
 * to the devices; this falls on the device service.
 */
public interface MastershipService
    extends ListenerService<MastershipEvent, MastershipListener> {

    long TIMEOUT_MILLIS = 3000;

    /**
     * Returns the role of the local node for the specified device, without
     * triggering master selection.
     *
     * @param deviceId the identifier of the device
     * @return role of the current node
     */
    MastershipRole getLocalRole(DeviceId deviceId);

    /**
     * Returns true if the local controller is the Master for the specified deviceId.
     *
     * @param deviceId the identifier of the device
     * @return true if local node is master; false otherwise
     */
    default boolean isLocalMaster(DeviceId deviceId) {
        return getLocalRole(deviceId) == MASTER;
    }

    /**
     * Returns the mastership status of the local controller for a given
     * device forcing master selection if necessary.
     *
     * @param deviceId the identifier of the device
     * @return future object of this controller instance role
     */
    CompletableFuture<MastershipRole> requestRoleFor(DeviceId deviceId);

    /**
     * Synchronous version of requestRoleFor. Returns the mastership status of
     * the local controller for a given device forcing master selection if necessary.
     *
     * @param deviceId the identifier of the device
     * @return the role of this controller instance
     */
    default MastershipRole requestRoleForSync(DeviceId deviceId) {
        return Tools.futureGetOrElse(requestRoleFor(deviceId),
                TIMEOUT_MILLIS, TimeUnit.MILLISECONDS, NONE);
    }

    /**
     * Abandons mastership of the specified device on the local node thus
     * forcing selection of a new master. If the local node is not a master
     * for this device, no master selection will occur.
     *
     * @param deviceId the identifier of the device
     * @return future that is completed when relinquish is complete
     */
    CompletableFuture<Void> relinquishMastership(DeviceId deviceId);

    /**
     * Synchronous version of relinquishMastership. Abandons mastership of the
     * specified device on the local node thus forcing selection of a new master.
     * If the local node is not a master for this device, no master selection will occur.
     *
     * @param deviceId the identifier of the device
     */
    default void relinquishMastershipSync(DeviceId deviceId) {
        Tools.futureGetOrElse(relinquishMastership(deviceId),
                TIMEOUT_MILLIS, TimeUnit.MILLISECONDS, null);
    }

    /**
     * Returns the current master for a given device.
     *
     * @param deviceId the identifier of the device
     * @return the ID of the master controller for the device
     */
    NodeId getMasterFor(DeviceId deviceId);

    /**
     * Returns controllers connected to a given device, in order of
     * preference. The first entry in the list is the current master.
     *
     * @param deviceId the identifier of the device
     * @return a list of controller IDs
     */
    RoleInfo getNodesFor(DeviceId deviceId);

    /**
     * Returns the devices for which a controller is master.
     * <p>
     * Returned Set may contain DeviceId which no longer exist in the system.
     *
     * @param nodeId the ID of the controller
     * @return a set of device IDs
     * @deprecated 1.6.0 Goldeneye release.
     */
    @Deprecated
    Set<DeviceId> getDevicesOf(NodeId nodeId);

}
