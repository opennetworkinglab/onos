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

 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.net.behaviour;

import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.Set;

/**
 * Handler behaviour for retrieving internal connectivity information.
 */
public interface InternalConnectivity extends HandlerBehaviour {
    /**
     * Test if two ports of the device can be internally connected.
     *
     * @param inputPort in port of device
     * @param outputPort out port of device
     * @return true if inputPort can be connected outputPort
     */
    boolean testConnectivity(PortNumber inputPort, PortNumber outputPort);

    /**
     * Returns the set of output ports that can be connected to inputPort.
     *
     * @param inputPort in port of device
     * @return list of output ports that can be connected to inputPort
     */
    Set<PortNumber> getOutputPorts(PortNumber inputPort);

    /**
     * Returns the set of input ports that can be connected to outputPort.
     *
     * @param outputPort out port of device
     * @return list of input ports that can be connected to outputPort
     */
    Set<PortNumber> getInputPorts(PortNumber outputPort);
}
