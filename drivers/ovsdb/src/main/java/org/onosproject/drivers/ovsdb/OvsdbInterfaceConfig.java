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

package org.onosproject.drivers.ovsdb;

import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.behaviour.PatchDescription;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.device.DeviceInterfaceDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbInterface;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * OVSDB-based implementation of interface config behaviour.
 */
public class OvsdbInterfaceConfig extends AbstractHandlerBehaviour implements InterfaceConfig {

    private final Logger log = getLogger(getClass());

    @Override
    public boolean addTunnelMode(String ifaceName, TunnelDescription tunnelDesc) {
        OvsdbInterface ovsdbIface = OvsdbInterface.builder(tunnelDesc).build();
        OvsdbClientService ovsdbClient = getOvsdbClient(handler());

        if (!tunnelDesc.deviceId().isPresent()) {
            log.warn("Device ID is required {}", tunnelDesc);
            return false;
        }
        return ovsdbClient.createInterface(tunnelDesc.deviceId().get(), ovsdbIface);
    }

    @Override
    public boolean removeTunnelMode(String ifaceName) {
        OvsdbClientService ovsdbClient = getOvsdbClient(handler());
        return ovsdbClient.dropInterface(ifaceName);
    }

    @Override
    public boolean addAccessMode(String ifaceName, VlanId vlanId) {
        // TODO implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean removeAccessMode(String ifaceName) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean addPatchMode(String ifaceName, PatchDescription patchDesc) {
        OvsdbInterface ovsdbIface = OvsdbInterface.builder(patchDesc).build();
        OvsdbClientService ovsdbClient = getOvsdbClient(handler());

        if (!patchDesc.deviceId().isPresent()) {
            log.warn("Device ID is required {}", patchDesc);
            return false;
        }
        return ovsdbClient.createInterface(patchDesc.deviceId().get(), ovsdbIface);
    }

    @Override
    public boolean removePatchMode(String ifaceName) {
        OvsdbClientService ovsdbClient = getOvsdbClient(handler());
        return ovsdbClient.dropInterface(ifaceName);
    }

    @Override
    public boolean addTrunkMode(String ifaceName, List<VlanId> vlanIds) {
        // TODO implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean removeTrunkMode(String ifaceName) {
        // TODO implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean addRateLimit(String ifaceName, short limit) {
        // TODO implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean removeRateLimit(String ifaceName) {
        // TODO implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<DeviceInterfaceDescription> getInterfaces() {
        // TODO implement
        throw new UnsupportedOperationException("Not implemented yet");
    }

    // OvsdbNodeId(IP) is used in the adaptor while DeviceId(ovsdb:IP)
    // is used in the core. So DeviceId need be changed to OvsdbNodeId.
    private OvsdbNodeId changeDeviceIdToNodeId(DeviceId deviceId) {
        String[] splits = deviceId.toString().split(":");
        if (splits.length < 1) {
            return null;
        }
        IpAddress ipAddress = IpAddress.valueOf(splits[1]);
        return new OvsdbNodeId(ipAddress, 0);
    }

    private OvsdbClientService getOvsdbClient(DriverHandler handler) {
        OvsdbController ovsController = handler.get(OvsdbController.class);
        OvsdbNodeId nodeId = changeDeviceIdToNodeId(handler.data().deviceId());

        return ovsController.getOvsdbClient(nodeId);
    }
}
