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
package org.onosproject.xosclient.api;

import java.util.Set;

/**
 * Service for interacting with XOS VTN ports.
 */
public interface VtnPortApi {

    /**
     * Returns all ports.
     *
     * @return set of ports
     */
    Set<VtnPort> vtnPorts();

    /**
     * Returns all ports with a given service.
     *
     * @param serviceId service id
     * @return set of ports
     */
    Set<VtnPort> vtnPorts(VtnServiceId serviceId);

    /**
     * Returns port information with port id.
     *
     * @param portId port id
     * @return vtn port; null if it fails to get port information
     */
    VtnPort vtnPort(VtnPortId portId);

    /**
     * Returns port information from OpenStack with port id.
     *
     * @param portId port id
     * @param osAccess OpenStack address for OS access
     * @return vtn port; null if it fails to get port information
     */
    // TODO remove this when XOS provides port information
    VtnPort vtnPort(VtnPortId portId, OpenStackAccess osAccess);

    /**
     * Returns port information from OpenStack with port name.
     *
     * @param portName port name
     * @param osAccess OpenStack address for OS access
     * @return vtn port; null if it fails to get port information
     */
    // TODO remove this when XOS provides port information
    VtnPort vtnPort(String portName, OpenStackAccess osAccess);
}
