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

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onosproject.iptopology.api.DevicePrefix;
import org.onosproject.iptopology.api.InterfaceIdentifier;
import org.onosproject.iptopology.api.IpDevice;
import org.onosproject.iptopology.api.DeviceIntf;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.Store;

import java.util.List;

/**
 * Manages inventory of ip devices; not intended for direct use.
 */
public interface IpDeviceStore extends Store<IpDeviceEvent, IpDeviceStoreDelegate> {

    /**
     * Returns the number of ip devices known to the system.
     *
     * @return number of ip devices
     */
    int getIpDeviceCount();

    /**
     * Returns an iterable collection of all ip devices known to the system.
     *
     * @return ip device collection
     */
    Iterable<IpDevice> getIpDevices();


    /**
     * Returns an ip device with the specified identifier.
     *
     * @param deviceId device identifier
     * @return ip device
     */
    IpDevice getIpDevice(DeviceId deviceId);

    /**
     * Creates a new infrastructure ip device, or updates an existing one using
     * the supplied device description.
     *
     * @param providerId        provider identifier
     * @param deviceId          device identifier
     * @param deviceDescription device description
     * @return ready to send event describing what occurred; null if no change
     */
    IpDeviceEvent createOrUpdateIpDevice(ProviderId providerId, DeviceId deviceId,
                                     IpDeviceDescription deviceDescription);

    /**
     * Administratively removes the specified ip device from the store.
     *
     * @param deviceId device to be removed
     * @return null if no such ip device
     */
    IpDeviceEvent removeIpDevice(DeviceId deviceId);

    /**
     * Updates the interface of the specified ip device using the given
     * list of interface descriptions. The list is assumed to be comprehensive.
     *
     * @param providerId            provider identifier
     * @param deviceId              ip device identifier
     * @param interfaceDescriptions list of interface descriptions
     * @return ready to send events describing what occurred; empty list if no change
     */
    List<IpDeviceEvent> updateInterfaces(ProviderId providerId, DeviceId deviceId,
                                  List<InterfaceDescription> interfaceDescriptions);

    /**
     * Administratively removes the specified interface from the store.
     *
     * @param deviceId device of the interfaces to be removed
     * @param interfaceDescriptions list of interface descriptions
     * @return ready to send events describing what occurred.
     */
    List<IpDeviceEvent> removeInterfaces(DeviceId deviceId, List<InterfaceDescription> interfaceDescriptions);

    /**
     * Returns the list of interfaces that belong to the specified device.
     *
     * @param deviceId device identifier
     * @return list of device interfaces
     */
    List<DeviceIntf> getInterfaces(DeviceId deviceId);

    /**
     * Returns the specified device interface.
     *
     * @param deviceId    device identifier
     * @param ipv4Address ipv4 address of the interface
     * @return device interface
     */
    DeviceIntf getInterface(DeviceId deviceId, Ip4Address ipv4Address);

    /**
     * Returns the specified device interface.
     *
     * @param deviceId    device identifier
     * @param ipv6Address ipv6 address of the interface
     * @return device interface
     */
    DeviceIntf getInterface(DeviceId deviceId, Ip6Address ipv6Address);

    /**
     * Returns the specified device interface.
     *
     * @param deviceId    device identifier
     * @param intfId      interface identifier of the interface
     * @return device interface
     */
    DeviceIntf getInterface(DeviceId deviceId, InterfaceIdentifier intfId);

    /**
     * Updates the prefix information of the specified ip device using the given
     * list of prefix descriptions. The list is assumed to be comprehensive.
     *
     * @param providerId           provider identifier
     * @param deviceId             ip device identifier
     * @param prefixDescriptions   list of prefix descriptions
     * @return ready to send events describing what occurred; empty list if no change
     */
    List<IpDeviceEvent>  updatePrefixes(ProviderId providerId, DeviceId deviceId,
                                        List<PrefixDescription> prefixDescriptions);

    /**
     * Administratively removes the specified prefix from the store.
     *
     * @param deviceId device of the prefix to be removed
     * @param prefixDescriptions list of prefix descriptions
     * @return ready to send events describing what occurred.
     */
    List<IpDeviceEvent> removePrefixes(DeviceId deviceId, List<PrefixDescription> prefixDescriptions);

    /**
     * Returns the list of prefixes that belong to the specified device.
     *
     * @param deviceId device identifier
     * @return list of device prefixes
     */
    List<DevicePrefix> getPrefixes(DeviceId deviceId);

}

