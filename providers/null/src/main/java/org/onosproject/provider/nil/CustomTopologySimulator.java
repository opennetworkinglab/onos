/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.provider.nil;

import com.google.common.collect.Maps;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.host.DefaultHostDescription;

import java.util.Map;
import java.util.Set;

import static org.onlab.util.Tools.toHex;
import static org.onosproject.provider.nil.NullProviders.SCHEME;

/**
 * Custom topology defined by a concise language.
 */
public class CustomTopologySimulator extends TopologySimulator {

    private int nextDeviceId = 0;
    private int nextHostId = 0;

    private Map<String, DeviceId> nameToId = Maps.newConcurrentMap();

    /**
     * Returns the next device id.
     *
     * @return the next device id
     */
    public DeviceId nextDeviceId() {
        return DeviceId.deviceId(SCHEME + ":" + toHex(++nextDeviceId));
    }

    /**
     * Returns the next host id.
     *
     * @return the next host id
     */
    public HostId nextHostId() {
        return HostId.hostId(MacAddress.valueOf(++nextHostId), VlanId.NONE);
    }

    /**
     * Returns the identifier of the device with the specified alias.
     *
     * @param name device name
     * @return device identifier
     */
    public DeviceId deviceId(String name) {
        return nameToId.get(name);
    }

    /**
     * Creates simulated device.
     *
     * @param id        device identifier
     * @param name      device name
     * @param type      device type
     * @param portCount number of device ports
     */
    public void createDevice(DeviceId id, String name, Device.Type type, int portCount) {
        int chassisId = Integer.parseInt(id.uri().getSchemeSpecificPart(), 16);
        createDevice(id, chassisId, type, portCount);
        nameToId.put(name, id);
    }

    /**
     * Creates simulated device.
     *
     * @param id        device identifier
     * @param name      device name
     * @param type      device type
     * @param hw        hardware revision
     * @param sw        software revision
     * @param portCount number of device ports
     */
    public void createDevice(DeviceId id, String name, Device.Type type,
                             String hw, String sw, int portCount) {
        int chassisId = Integer.parseInt(id.uri().getSchemeSpecificPart(), 16);
        createDevice(id, chassisId, type, hw, sw, portCount);
        nameToId.put(name, id);
    }

    /**
     * Creates a simulated host.
     *
     * @param hostId   host identifier
     * @param location host location
     * @param hostIp   host IP address
     */
    public void createHost(HostId hostId, HostLocation location, IpAddress hostIp) {
        DefaultHostDescription description =
                new DefaultHostDescription(hostId.mac(), hostId.vlanId(), location, hostIp);
        hostProviderService.hostDetected(hostId, description, false);
    }

    /**
     * Creates a simulated multi-homed host.
     *
     * @param hostId   host identifier
     * @param locations host locations
     * @param hostIps   host IP addresses
     */
    public void createHost(HostId hostId, Set<HostLocation> locations, Set<IpAddress> hostIps) {
        DefaultHostDescription description =
                new DefaultHostDescription(hostId.mac(), hostId.vlanId(), locations, hostIps, false);
        hostProviderService.hostDetected(hostId, description, false);
    }

    @Override
    protected void createDevices() {
    }

    @Override
    protected void createLinks() {
    }

    @Override
    protected void createHosts() {
    }

    @Override
    public void tearDownTopology() {
        super.tearDownTopology();
        nextDeviceId = 0;
        nextHostId = 0;
        nameToId.clear();
    }

}
