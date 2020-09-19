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

package org.onosproject.drivers.odtn;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.InternalConnectivity;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import java.util.Set;
import java.util.stream.Collectors;

public class FullMeshInternalConnectivity
    extends AbstractHandlerBehaviour implements InternalConnectivity {

    /**
     * Returnung true for all pairs this implements a device with full connectivity.
     *
     * @param inputPort in port
     * @param outputPort out port
     * @return true if connectivity is allowed
     */
    @Override
    public boolean testConnectivity(PortNumber inputPort, PortNumber outputPort) {
        //It is not allowed touse the same port as input and output
        if (inputPort.equals(outputPort)) {
            return false;
        }
        return true;
    }

    /**
     * To be implemented.
     *
     * @param inputPort in port
     * @return Set of out ports that can be connected to specified in port
     */
    @Override
    public Set<PortNumber> getOutputPorts(PortNumber inputPort) {
        Set<PortNumber> ports;

        //Returns all the device ports, not including input port itself
        DeviceService deviceService = this.handler().get(DeviceService.class);
        ports = deviceService.getPorts(did()).stream()
                .filter(p -> !p.equals(inputPort))
                .map(p -> p.number())
                .collect(Collectors.toSet());

        return ports;
    }

    /**
     * To be implemented.
     *
     * @param outputPort out port
     * @return Set of in ports that can be connected to specified out port
     */
    @Override
    public Set<PortNumber> getInputPorts(PortNumber outputPort) {
        Set<PortNumber> ports;

        //Returns all the device ports, not including output port itself
        DeviceService deviceService = this.handler().get(DeviceService.class);
        ports = deviceService.getPorts(did()).stream()
                .filter(p -> !p.equals(outputPort))
                .map(p -> p.number())
                .collect(Collectors.toSet());

        return ports;
    }

    /**
     * Get the deviceId for which the methods apply.
     *
     * @return The deviceId as contained in the handler data
     */
    private DeviceId did() {
        return handler().data().deviceId();
    }
}
