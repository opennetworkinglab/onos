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
 * Factory class for Connection objects based on the OpenRoadmFlowRule.
 */
public final class OpenRoadmConnectionFactory {

    /**
     * Static method to create an OpenROADM connection.
     *
     * @param openRoadmName name of the Connection.
     * @param xc the associated OpenRoadmFlowRule.
     * @param deviceService ONOS device service.
     * @return the OpenRoadmConnectionObject
     * @throws IllegalArgumentException
     *
     * Based on the cross-connection type, the method allocates and
     * Add, Drop or Express connection object (or local, from client to client).
     */
    public static OpenRoadmConnection create(String openRoadmName,
                                             OpenRoadmFlowRule xc,
                                             DeviceService deviceService)
      throws IllegalArgumentException {
        switch (xc.type()) {
            case EXPRESS_LINK:
                return new OpenRoadmExpressConnection(openRoadmName, xc,
                                                      deviceService);
            case ADD_LINK:
                return new OpenRoadmAddConnection(openRoadmName, xc,
                                                  deviceService);
            case DROP_LINK:
                return new OpenRoadmDropConnection(openRoadmName, xc,
                                                   deviceService);
            case LOCAL:
                return new OpenRoadmLocalConnection(openRoadmName, xc,
                                                    deviceService);
            default:
                throw new IllegalArgumentException(
                  "Unknown OpenRoadmFlowRule type");
        }
    }

    private OpenRoadmConnectionFactory() {
        // Utility classes should not have a public or default constructor.
        // [HideUtilityClassConstructor]
    }
}
