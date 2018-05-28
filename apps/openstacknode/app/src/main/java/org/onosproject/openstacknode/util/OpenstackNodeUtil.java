/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknode.util;

import org.onosproject.net.device.DeviceService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An utility that used in openstack node app.
 */
public final class OpenstackNodeUtil {
    protected static final Logger log = LoggerFactory.getLogger(OpenstackNodeUtil.class);

    /**
     * Prevents object instantiation from external.
     */
    private OpenstackNodeUtil() {
    }

    /**
     * Checks whether the controller has a connection with an OVSDB that resides
     * inside the given openstack node.
     *
     * @param osNode openstack node
     * @param ovsdbPort ovsdb port
     * @param ovsdbController ovsdb controller
     * @param deviceService device service
     * @return true if the controller is connected to the OVSDB, false otherwise
     */
    public static boolean isOvsdbConnected(OpenstackNode osNode,
                                           int ovsdbPort,
                                           OvsdbController ovsdbController,
                                           DeviceService deviceService) {
        OvsdbNodeId ovsdb = new OvsdbNodeId(osNode.managementIp(), ovsdbPort);
        OvsdbClientService client = ovsdbController.getOvsdbClient(ovsdb);
        return deviceService.isAvailable(osNode.ovsdb()) &&
                client != null &&
                client.isConnected();
    }
}
