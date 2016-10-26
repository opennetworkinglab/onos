/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PortConfigBehaviour;
import org.onosproject.net.behaviour.QosDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * OVSDB-based implementation of port config behaviour.
 */
public class OvsdbPortConfig extends AbstractHandlerBehaviour implements PortConfigBehaviour {

    private final Logger log = getLogger(getClass());

    @Override
    public void applyQoS(PortDescription portDesc, QosDescription qosDesc) {
        OvsdbClientService ovsdbClient = getOvsdbClient(handler());
        if (ovsdbClient == null) {
            return;
        }
        ovsdbClient.applyQos(portDesc.portNumber(), qosDesc.qosId().name());
    }

    @Override
    public void removeQoS(PortNumber portNumber) {
        OvsdbClientService ovsdbClient = getOvsdbClient(handler());
        if (ovsdbClient == null) {
            return;
        }
        ovsdbClient.removeQos(portNumber);
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

