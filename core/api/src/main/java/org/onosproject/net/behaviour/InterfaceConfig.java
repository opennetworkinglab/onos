/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.behaviour;

import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceInterfaceDescription;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.List;

/**
 * Means to configure interfaces on devices.
 */
public interface InterfaceConfig extends HandlerBehaviour {

    /**
     * Adds an access interface to a VLAN.
     *
     * @param deviceId the device ID
     * @param intf the name of the interface
     * @param vlanId the VLAN ID
     * @return the result of operation
     */
    boolean addAccessInterface(DeviceId deviceId, String intf, VlanId vlanId);

    /**
     * Removes an access interface to a VLAN.
     *
     * @param deviceId the device ID
     * @param intf the name of the interface
     * @return the result of operation
     */
    boolean removeAccessInterface(DeviceId deviceId, String intf);

    /**
     *  Adds a trunk interface for VLANs.
     *
     * @param deviceId the device ID
     * @param intf the name of the interface
     * @param vlanIds the VLAN IDs
     * @return the result of operation
     */
    boolean addTrunkInterface(DeviceId deviceId, String intf, List<VlanId> vlanIds);

    /**
     *  Removes trunk mode configuration from an interface.
     *
     * @param deviceId the device ID
     * @param intf the name of the interface
     * @return the result of operation
     */
    boolean removeTrunkInterface(DeviceId deviceId, String intf);

    /**
     * Provides the interfaces configured on a device.
     *
     * @param deviceId the device ID
     * @return the list of the configured interfaces
     */
    List<DeviceInterfaceDescription> getInterfaces(DeviceId deviceId);

    /**
     *  TODO Addition of more methods to make the behavior symmetrical.
     *  Methods getInterfacesForVlan, getVlansForInterface, getTrunkforInterface,
     *  getInterfacesForTrunk should be added to complete the behavior.
     */

}
