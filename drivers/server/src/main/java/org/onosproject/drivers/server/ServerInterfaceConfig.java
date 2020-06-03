/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.drivers.server;

import org.onlab.packet.VlanId;
import org.onosproject.drivers.server.devices.RestServerSBDevice;
import org.onosproject.drivers.server.devices.nic.NicDevice;
import org.onosproject.drivers.server.devices.nic.NicRxFilter.RxFilter;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.behaviour.PatchDescription;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.device.DefaultDeviceInterfaceDescription;
import org.onosproject.net.device.DeviceInterfaceDescription;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_ID_NULL;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_NULL;
import static org.onosproject.net.device.DeviceInterfaceDescription.Mode;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of interface config behaviour for server devices.
 */
public class ServerInterfaceConfig
        extends BasicServerDriver
        implements InterfaceConfig {

    private final Logger log = getLogger(getClass());

    private static final boolean RATE_LIMIT_STATUS = false;
    private static final short NO_LIMIT = -1;

    public ServerInterfaceConfig() {
        super();
        log.debug("Started");
    }

    @Override
    public boolean addTunnelMode(String ifaceName, TunnelDescription tunnelDesc) {
        throw new UnsupportedOperationException("Interface operation not supported");
    }

    @Override
    public boolean removeTunnelMode(String ifaceName) {
        throw new UnsupportedOperationException("Interface operation not supported");
    }

    @Override
    public boolean addAccessMode(String ifaceName, VlanId vlanId) {
        throw new UnsupportedOperationException("Interface operation not supported");
    }

    @Override
    public boolean removeAccessMode(String ifaceName) {
        throw new UnsupportedOperationException("Interface operation not supported");
    }

    @Override
    public boolean addPatchMode(String ifaceName, PatchDescription patchDesc) {
        throw new UnsupportedOperationException("Interface operation not supported");
    }

    @Override
    public boolean removePatchMode(String ifaceName) {
        throw new UnsupportedOperationException("Interface operation not supported");
    }

    @Override
    public boolean addTrunkMode(String ifaceName, List<VlanId> vlanIds) {
        throw new UnsupportedOperationException("Interface operation not supported");
    }

    @Override
    public boolean removeTrunkMode(String ifaceName) {
        throw new UnsupportedOperationException("Interface operation not supported");
    }

    @Override
    public boolean addRateLimit(String ifaceName, short limit) {
        throw new UnsupportedOperationException("Interface operation not supported");
    }

    @Override
    public boolean removeRateLimit(String ifaceName) {
        throw new UnsupportedOperationException("Interface operation not supported");
    }

    @Override
    public List<DeviceInterfaceDescription> getInterfaces() {
        // Retrieve the device ID
        DeviceId deviceId = getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // .. and the device itself
        RestServerSBDevice device = null;
        try {
            device = (RestServerSBDevice) getDevice(deviceId);
        } catch (ClassCastException ccEx) {
            log.error("Failed to get interfaces for device {}", deviceId);
            return Collections.EMPTY_LIST;
        }

        if (device == null) {
            log.error("No device with ID {} is available for interface discovery", deviceId);
            return Collections.EMPTY_LIST;
        }
        if ((device.nics() == null) || (device.nics().size() == 0)) {
            log.error("No interfaces available on {}", deviceId);
            return Collections.EMPTY_LIST;
        }

        // List of port descriptions to return
        List<DeviceInterfaceDescription> intfDescriptions = Lists.newArrayList();

        // Sorted list of NIC ports
        Set<NicDevice> nics = new TreeSet(device.nics());

        // Iterate through the NICs of this device to populate the list
        for (NicDevice nic : nics) {
            List<VlanId> devVlanIds = getVlanIdListForDevice(nic);
            Mode devMode = getDeviceMode(devVlanIds);

            // Create an interface description and add it to the list
            intfDescriptions.add(
                new DefaultDeviceInterfaceDescription(
                    nic.name(), devMode, devVlanIds, RATE_LIMIT_STATUS, NO_LIMIT));
        }

        return ImmutableList.copyOf(intfDescriptions);
    }

    /**
     * Returns a list of VLAN IDs associated with a NIC device.
     *
     * @param nic the NIC device to be queried
     * @return a list of VLAN IDs (if any)
     */
    private List<VlanId> getVlanIdListForDevice(NicDevice nic) {
        checkNotNull(nic, MSG_DEVICE_NULL);
        List<VlanId> vlanIds = Lists.newArrayList();

        short vlanIdValue = 0;
        for (RxFilter rxFilter : nic.rxFilterMechanisms().rxFilters()) {
            if (rxFilter == RxFilter.VLAN) {
                vlanIds.add(VlanId.vlanId(vlanIdValue++));
            }
        }

        return ImmutableList.copyOf(vlanIds);
    }

    /**
     * Returns the interface mode of a NIC device, by looking at its VLAN IDs.
     *
     * @param vlanIds a list of VLAN IDs associated with the device
     * @return interface mode
     */
    private Mode getDeviceMode(List<VlanId> vlanIds) {
        if (vlanIds.size() == 1) {
            return Mode.ACCESS;
        } else if (vlanIds.size() > 1) {
            return Mode.TRUNK;
        }

        return Mode.NORMAL;
    }

}
