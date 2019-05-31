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

 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */
package org.onosproject.drivers.odtn.openroadm;

import org.onosproject.net.device.DeviceService;

/**
 * Class that models an EXPRESS connection.
 *
 */
public class OpenRoadmExpressConnection extends OpenRoadmConnection {

    /**
     * Constructor for an Express connection (from line port to line port).
     *
     * @param openRoadmName name of the Connection.
     * @param xc the associated OpenRoadmFlowRule.
     * @param deviceService ONOS device service.
     */
    public OpenRoadmExpressConnection(String openRoadmName, OpenRoadmFlowRule xc,
                                      DeviceService deviceService) {
        super(openRoadmName, xc, deviceService);
    }
}
