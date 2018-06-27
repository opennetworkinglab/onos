/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.server.devices.nic;

import org.onlab.packet.MacAddress;

import static org.onosproject.net.Port.Type;

/**
 * Represents an abstraction of a
 * network interface card (NIC) device in ONOS.
 */
public interface NicDevice extends Comparable {

    /**
     * Returns the ID of this NIC.
     *
     * @return NIC ID
     */
    String id();

    /**
     * Returns the port number of this NIC.
     *
     * @return integer port number for the NIC
     */
    int port();

    /**
     * Returns the type of the port of this NIC.
     *
     * @return port type
     */
    Type portType();

    /**
     * Returns the speed of the NIC in Mbps.
     *
     * @return integer NIC speed in Mbps
     */
    long speed();

    /**
     * Returns the current status of the NIC.
     *
     * @return boolean NIC status (up=true, down=false)
     */
    boolean status();

    /**
     * Sets the current status of the NIC.
     *
     * @param status boolean NIC status (up=true, down=false)
     */
    void setStatus(boolean status);

    /**
     * Returns the MAC address of the NIC.
     *
     * @return MacAddress hardware address of the NIC
     */
    MacAddress macAddress();

    /**
     * Returns the Rx filter mechanisms supported by the NIC.
     *
     * @return Rx filter mechanisms
     */
    NicRxFilter rxFilterMechanisms();

    /**
     * Sets the Rx filter mechanisms supported by the NIC.
     *
     * @param rxFilterMechanisms Rx filter mechanisms
     */
    void setRxFilterMechanisms(NicRxFilter rxFilterMechanisms);

    /**
     * Adds a new Rx filter to the NIC.
     *
     * @param rxFilter an Rx filter to be added to the set
     */
    void addRxFilterMechanism(NicRxFilter.RxFilter rxFilter);

}
