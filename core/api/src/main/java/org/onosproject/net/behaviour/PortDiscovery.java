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

package org.onosproject.net.behaviour;

import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.List;

/**
 * Discovers the set of ports from a device through a device specific protocol.
 * The returned ports are not retrieved from the information stored in ONOS.
 *
 * @deprecated 1.6.0 Goldeneye. Use DeviceDescriptionDiscovery instead
 */
@Deprecated
public interface PortDiscovery extends HandlerBehaviour {

    /**
     * Retrieves the set of ports from a device.
     *
     * @return a set of port descriptions.
     * @deprecated 1.6.0 Goldeneye. Use DeviceDescriptionDiscovery instead
     */
    @Deprecated
    List<PortDescription> getPorts();
}
