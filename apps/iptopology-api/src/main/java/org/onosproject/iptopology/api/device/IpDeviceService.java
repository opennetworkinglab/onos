/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.iptopology.api.device;

import org.onosproject.event.ListenerService;
import org.onosproject.iptopology.api.DeviceIntf;
import org.onosproject.iptopology.api.DevicePrefix;
import org.onosproject.iptopology.api.InterfaceIdentifier;
import org.onosproject.iptopology.api.IpDevice;
import org.onosproject.net.DeviceId;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;

import java.util.List;

/**
 * Service for interacting with the inventory of ip devices.
 */
public interface IpDeviceService
    extends ListenerService<IpDeviceEvent, IpDeviceListener> {

    /**
     * Returns the number of ip devices known to the system.
     *
     * @return number of infrastructure devices
     */
    int getIpDeviceCount();

    /**
     * Returns a collection of the currently known ip
     * devices.
     *
     * @return collection of devices
     */
    Iterable<IpDevice> getIpDevices();

    /**
     * Returns a collection of the currently known ip
     * devices by device type.
     *
     * @param type device type
     * @return collection of devices
     */
    Iterable<IpDevice> getIpDevices(IpDevice.Type type);


    /**
     * Returns the ip device with the specified identifier.
     *
     * @param deviceId device identifier
     * @return device or null if one with the given identifier is not known
     */
    IpDevice getIpDevice(DeviceId deviceId);

    /**
     * Returns the list of interfaces associated with the device.
     *
     * @param deviceId device identifier
     * @return list of device interfaces
     */
    List<DeviceIntf> getInterfaces(DeviceId deviceId);

    /**
     * Returns the interface with the specified ipv4 address and hosted by the given device.
     *
     * @param deviceId   device identifier
     * @param ipv4Address ipv4 address
     * @return device interface
     */
    DeviceIntf getInterface(DeviceId deviceId, Ip4Address ipv4Address);

    /**
     * Returns the interface with the specified ipv6 address and hosted by the given device.
     *
     * @param deviceId   device identifier
     * @param ipv6Address ipv6 address
     * @return device interface
     */
    DeviceIntf getInterface(DeviceId deviceId, Ip6Address ipv6Address);

    /**
     * Returns the interface with the specified interface id and hosted by the given device.
     *
     * @param deviceId   device identifier
     * @param intfId interface id
     * @return device interface
     */
    DeviceIntf getInterface(DeviceId deviceId, InterfaceIdentifier intfId);

    /**
     * Returns the list of ip prefix associated with the device.
     *
     * @param deviceId device identifier
     * @return list of device prefixes
     */
    List<DevicePrefix> getPrefixes(DeviceId deviceId);

}
