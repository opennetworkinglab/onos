/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.netconf;

import org.onlab.packet.IpAddress;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Abstraction of an NETCONF controller. Serves as a one stop shop for obtaining
 * NetconfDevice and (un)register listeners on NETCONF device events.
 */
public interface NetconfController {

    /**
     * Adds Device Event Listener.
     *
     * @param listener node listener
     */
    void addDeviceListener(NetconfDeviceListener listener);

    /**
     * Removes Device Listener.
     *
     * @param listener node listener
     */
    void removeDeviceListener(NetconfDeviceListener listener);

    /**
     * Tries to connect to a specific NETCONF device, if the connection is succesful
     * it creates and adds the device to the ONOS core as a NetconfDevice.
     *
     * @param deviceId deviceId of the device to connect
     * @return NetconfDevice Netconf device
     * @throws NetconfException when device is not available
     */
    NetconfDevice connectDevice(DeviceId deviceId) throws NetconfException;

    /**
     * Tries to connect to a specific NETCONF device, if the connection is succesful
     * it creates and adds the device to the ONOS core as a NetconfDevice.
     * If isMaster true: Will create two sessions for a device : secure transport session and proxy session.
     * If isMaster false: Will create only proxy session.
     * @param deviceId deviceId of the device to connect
     * @param isMaster if the controller is master for the device
     * @return NetconfDevice Netconf device
     * @throws NetconfException when device is not available
     */
    default NetconfDevice connectDevice(DeviceId deviceId, boolean isMaster) throws NetconfException {
        return connectDevice(deviceId);
    }

    /**
     * Disconnects a Netconf device and removes it from the core.
     *
     * @param deviceId id of the device to remove
     * @param remove   true if device is to be removed from core
     */
    void disconnectDevice(DeviceId deviceId, boolean remove);

    /**
     * Removes a Netconf device from the core.
     *
     * @param deviceId id of the device to remove
     */
    void removeDevice(DeviceId deviceId);

    /**
     * Gets all the nodes information.
     *
     * @return map of devices
     */
    Map<DeviceId, NetconfDevice> getDevicesMap();

    /**
     * Gets all Netconf Devices.
     *
     * @return List of all the NetconfDevices Ids
     */
    Set<DeviceId> getNetconfDevices();

    /**
     * Gets a Netconf Device by node identifier.
     *
     * @param deviceInfo node identifier
     * @return NetconfDevice Netconf device
     */
    NetconfDevice getNetconfDevice(DeviceId deviceInfo);

    /**
     * Gets a Netconf Device by node identifier.
     *
     * @param ip   device ip
     * @param port device port
     * @return NetconfDevice Netconf device
     */
    NetconfDevice getNetconfDevice(IpAddress ip, int port);

    /**
     * Gets a Netconf Device by node identifier.
     *
     * @param ip   device ip
     * @param port device port
     * @param path device path
     * @return NetconfDevice Netconf device
     */
    NetconfDevice getNetconfDevice(IpAddress ip, int port, String path);

    /**
     * If master, will execute the call locally else will use
     * clusterCommunicationManager to execute at master controller.
     * Meant only for internal synchronization and not to be used by applications.
     *
     * @param proxyMessage proxy message
     * @param <T> return type
     * @return Completable future of class T
     * @throws NetconfException netconf exception
     */
    default <T> CompletableFuture<T> executeAtMaster(NetconfProxyMessage proxyMessage) throws NetconfException {
        CompletableFuture<T> errorFuture = new CompletableFuture<>();
        errorFuture.completeExceptionally(new NetconfException("Method executeAtMaster not implemented"));
        return errorFuture;
    }

    /**
     * If master, will execute the call locally else will use
     * clusterCommunicationManager to execute at master controller.
     * Meant only for internal synchronization and not to be used by applications.
     *
     * @param deviceId deviceId of device
     * @return true or false
     */
    default boolean pingDevice(DeviceId deviceId) {
        return false;
    }

    /**
     * Get a contoller node Id .
     *
     * @return controller node Id
     */
    default NodeId getLocalNodeId() {
        return null;
    }
}
