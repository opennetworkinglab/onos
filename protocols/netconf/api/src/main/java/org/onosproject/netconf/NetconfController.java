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

package org.onosproject.netconf;

import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;

import java.io.IOException;
import java.util.Map;

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
     * @param deviceInfo info about the device to add
     * @return NetconfDevice Netconf device
     */
    NetconfDevice connectDevice(NetconfDeviceInfo deviceInfo) throws IOException;

    /**
     * Removes a Netconf device.
     *
     * @param deviceInfo info about the device to remove
     */
    void removeDevice(NetconfDeviceInfo deviceInfo);

    /**
     * Gets all the nodes information.
     *
     * @return map of devices
     */
    Map<DeviceId, NetconfDevice> getDevicesMap();

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

}
