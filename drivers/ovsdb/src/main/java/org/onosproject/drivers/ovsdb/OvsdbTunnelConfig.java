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

package org.onosproject.drivers.ovsdb;

import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.TunnelConfig;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbInterface;
import org.onosproject.ovsdb.controller.OvsdbNodeId;

import java.util.Collection;
import java.util.Collections;

/**
 * OVSDB-based implementation of tunnel config behaviour.
 *
 * @deprecated version 1.7.0 - Hummingbird; use interface config instead
 */
@Deprecated
public class OvsdbTunnelConfig extends AbstractHandlerBehaviour
        implements TunnelConfig {

    @Override
    public boolean createTunnelInterface(BridgeName bridgeName, TunnelDescription tunnel) {
        DriverHandler handler = handler();
        OvsdbClientService ovsdbNode = getOvsdbNode(handler);

        OvsdbInterface ovsdbIface = OvsdbInterface.builder(tunnel).build();
        return ovsdbNode.createInterface(bridgeName.name(), ovsdbIface);
    }

    @Override
    public void removeTunnel(TunnelDescription tunnel) {
        DriverHandler handler = handler();
        OvsdbClientService ovsdbNode = getOvsdbNode(handler);
        ovsdbNode.dropInterface(tunnel.ifaceName());
    }

    @Override
    public void updateTunnel(TunnelDescription tunnel) {
        // TODO Auto-generated method stub

    }

    @Override
    public Collection<TunnelDescription> getTunnels() {
        return Collections.emptyList();
    }

    // OvsdbNodeId(IP) is used in the adaptor while DeviceId(ovsdb:IP)
    // is used in the core. So DeviceId need be changed to OvsdbNodeId.
    private OvsdbNodeId changeDeviceIdToNodeId(DeviceId deviceId) {
        String[] splits = deviceId.toString().split(":");
        if (splits == null || splits.length < 1) {
            return null;
        }
        IpAddress ipAddress = IpAddress.valueOf(splits[1]);
        return new OvsdbNodeId(ipAddress, 0);
    }

    private OvsdbClientService getOvsdbNode(DriverHandler handler) {
        OvsdbController ovsController = handler.get(OvsdbController.class);
        DeviceId deviceId = handler.data().deviceId();
        OvsdbNodeId nodeId = changeDeviceIdToNodeId(deviceId);
        return ovsController.getOvsdbClient(nodeId);
    }
}
