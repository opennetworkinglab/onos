/*
 * Copyright 2015-present Open Networking Foundation
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.onosproject.net.PortNumber;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.HandlerBehaviour;

/**
 * Behaviour for handling various drivers for bridge configurations.
 */
public interface BridgeConfig extends HandlerBehaviour {

    /**
     * Adds a bridge with a given description.
     *
     * @param bridgeDescription bridge description
     * @return true if succeeds, or false
     */
    boolean addBridge(BridgeDescription bridgeDescription);

    /**
     * Remove a bridge.
     *
     * @param bridgeName bridge name
     */
    void deleteBridge(BridgeName bridgeName);

    /**
     * Remove a bridge.
     *
     * @return bridge collection
     */
    Collection<BridgeDescription> getBridges();

    /**
     * Adds a port to a given bridge.
     *
     * @param bridgeName bridge name
     * @param portName port name
     */
    void addPort(BridgeName bridgeName, String portName);

    /**
     * Adds ports to a given bridge.
     *
     * @param bridgeName bridge name
     * @param portNames list port name
     */
    default void addPorts(BridgeName bridgeName, List<String> portNames) {
        List<String> portsList = new ArrayList<>();
        for (String portName : portNames) {
                portsList.add(portName);
        }
    }

    /**
     * Removes a port from a given bridge.
     *
     * @param bridgeName bridge name
     * @param portName port name
     */
    void deletePort(BridgeName bridgeName, String portName);

    /**
     * Deletes ports to a given bridge.
     * @param bridgeName bridge name
     * @param portNames list port names
     */
    default void deletePorts(BridgeName bridgeName, List<String> portNames) {
        for (String portName : portNames) {
            deletePort(bridgeName, portName);
        }
    }


    /**
     * Delete a logical/virtual port.
     *
     * @return collection of port
     */
    Collection<PortDescription> getPorts();

    /**
     * Get a collection of port.
     *
     * @return portNumbers set of PortNumber
     */
    Set<PortNumber> getPortNumbers();

    /**
     * Get logical/virtual ports by ifaceIds.
     *
     * @param ifaceIds the ifaceid that needed
     * @return list of PortNumber
     */
    List<PortNumber> getLocalPorts(Iterable<String> ifaceIds);
}
