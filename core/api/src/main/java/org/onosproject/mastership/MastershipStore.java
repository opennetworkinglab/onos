/*
 * Copyright 2014-present Open Networking Foundation
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

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.store.Store;

/**
 * Manages inventory of mastership roles for devices, across controller
 * instances; not intended for direct use.
 */
public interface MastershipStore extends Store<MastershipEvent, MastershipStoreDelegate> {

    // three things to map: NodeId, DeviceId, MastershipRole

    /**
     * Requests role of the local node for the specified device.
     *
     * @param deviceId device identifier
     * @return established or newly negotiated mastership role
     */
    CompletableFuture<MastershipRole> requestRole(DeviceId deviceId);

    /**
     * Returns the role of a device for a specific controller instance.
     *
     * @param nodeId   the instance identifier
     * @param deviceId the device identifiers
     * @return the role
     */
    MastershipRole getRole(NodeId nodeId, DeviceId deviceId);

    /**
     * Returns the master for a device.
     *
     * @param deviceId the device identifier
     * @return the instance identifier of the master
     */
    NodeId getMaster(DeviceId deviceId);

    /**
     * Returns the master and backup nodes for a device.
     *
     * @param deviceId the device identifier
     * @return a RoleInfo containing controller IDs
     */
    RoleInfo getNodes(DeviceId deviceId);

    /**
     * Returns the devices that a controller instance is master of.
     *
     * @param nodeId the instance identifier
     * @return a set of device identifiers
     */
    Set<DeviceId> getDevices(NodeId nodeId);

    /**
     * Sets a device's role for a specified controller instance.
     *
     * @param nodeId   controller instance identifier
     * @param deviceId device identifier
     * @return a mastership event
     */
    CompletableFuture<MastershipEvent> setMaster(NodeId nodeId, DeviceId deviceId);

    /**
     * Returns the current master and number of past mastership hand-offs
     * (terms) for a device.
     *
     * @param deviceId the device identifier
     * @return the current master's ID and the term value for device, or null
     */
    MastershipTerm getTermFor(DeviceId deviceId);

    /**
     * Returns the mastership info for the given device.
     *
     * @param deviceId the device for which to return the mastership info
     * @return the mastership info for the given device
     */
    MastershipInfo getMastership(DeviceId deviceId);

    /**
     * Sets a controller instance's mastership role to STANDBY for a device.
     * If the role is MASTER, another controller instance will be selected
     * as a candidate master.
     *
     * @param nodeId   the controller instance identifier
     * @param deviceId device to revoke mastership role for
     * @return a mastership event
     */
    CompletableFuture<MastershipEvent> setStandby(NodeId nodeId, DeviceId deviceId);

    /**
     * Allows a controller instance to give up its current role for a device.
     * If the role is MASTER, another controller instance will be selected
     * as a candidate master.
     *
     * @param nodeId   the controller instance identifier
     * @param deviceId device to revoke mastership role for
     * @return a mastership event
     */
    CompletableFuture<MastershipEvent> relinquishRole(NodeId nodeId, DeviceId deviceId);

    /**
     * Removes all the roles for the specified controller instance.
     * If the role was MASTER, another controller instance will be selected
     * as a candidate master.
     *
     * @param nodeId the controller instance identifier
     */
    void relinquishAllRole(NodeId nodeId);

    /**
     * Attempts to demote a node to the bottom of the backup list. It is not allowed
     * to demote the current master
     *
     * @param instance controller instance identifier
     * @param deviceId device identifier
     */
    void demote(NodeId instance, DeviceId deviceId);
}
