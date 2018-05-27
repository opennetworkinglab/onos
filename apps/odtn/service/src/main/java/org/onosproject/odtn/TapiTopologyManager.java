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

package org.onosproject.odtn;

import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.net.Port;

/**
 * ODTN Tapi topology manager application interface.
 */
public interface TapiTopologyManager {

    /**
     * DEVICE_ADDED event handler.
     *
     * @param device device to be added
     */
    void addDevice(Device device);

    /**
     * DEVICE_REMOVED event handler.
     *
     * @param device device to be removed
     */
    void removeDevice(Device device);

    /**
     * LINK_ADDED event handler.
     *
     * @param link link to be added
     */
    void addLink(Link link);

    /**
     * LINK_REMOVED event handler.
     *
     * @param link link to be removed
     */
    void removeLink(Link link);

    /**
     * PORT_ADDED event handler.
     *
     * @param port port to be added
     */
    void addPort(Port port);

    /**
     * PORT_REMOVED event handler.
     *
     * @param port port to be removed
     */
    void removePort(Port port);
}
