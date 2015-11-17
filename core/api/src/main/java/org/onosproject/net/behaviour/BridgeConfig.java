/*
 * Copyright 2015 Open Networking Laboratory
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
     * Add a bridge.
     *
     * @param bridgeName bridge name
     */
    void addBridge(BridgeName bridgeName);

    /**
     * Adds a bridge with given bridge name, dpid and exPortName.
     *
     * @param bridgeName bridge name
     * @param dpid dpid
     * @param exPortName external port name
     */
    void addBridge(BridgeName bridgeName, String dpid, String exPortName);

    /**
     * Adds a bridge with given bridge name and dpid, and sets the controller
     * of the bridge with given controllers.
     *
     * @param bridgeName bridge name
     * @param dpid dpid
     * @param controllers list of controller
     * @return true if succeeds, fail otherwise
     */
    boolean addBridge(BridgeName bridgeName, String dpid, List<ControllerInfo> controllers);

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
     * Add a logical/virtual port.
     *
     * @param port port number
     */
    void addPort(PortDescription port);

    /**
     * Delete a logical/virtual port.
     *
     * @param port port number
     */
    void deletePort(PortDescription port);

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
