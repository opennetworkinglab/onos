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

import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderService;

import java.util.List;

/**
 * Service through which ip device providers can inject ip device information into
 * the core.
 */
public interface IpDeviceProviderService extends ProviderService<IpDeviceProvider> {

    /**
     * Signals the core that an ip device is added or updated with IP topology information.
     *
     * @param deviceId device identifier
     * @param deviceDescription information about network ip device
     */
    void addOrUpdateIpDevice(DeviceId deviceId, IpDeviceDescription deviceDescription);

    /**
     * Signals the core that an ip device is removed.
     *
     * @param deviceId identity of the ip device to be removed
     */
    void removeIpDevice(DeviceId deviceId);

    /**
     * Sends information about all interfaces of a device. It is up to the core to
     * determine what has changed.
     *
     * @param deviceId         identity of the ip device
     * @param interfaceDescriptions list of device interfaces
     */
    void updateInterfaces(DeviceId deviceId, List<InterfaceDescription> interfaceDescriptions);

    /**
     * signals interfaces of a device is deleted.
     *
     * @param deviceId         identity of the ip device
     * @param interfaceDescriptions list of device interfaces
     */
    void removeInterfaces(DeviceId deviceId, List<InterfaceDescription> interfaceDescriptions);

    /**
     * Sends information about all ip prefix of a device. It is up to the core to
     * determine what has changed.
     *
     * @param deviceId         identity of the ip device
     * @param prefixDescriptions list of device ip prefixes
     */
    void updatePrefixes(DeviceId deviceId, List<PrefixDescription> prefixDescriptions);

    /**
     * signals ip prefix of a device is deleted.
     *
     * @param deviceId         identity of the ip device
     * @param prefixDescriptions list of device ip prefixes
     */
    void removePrefixes(DeviceId deviceId, List<PrefixDescription> prefixDescriptions);

}
