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
 * network interface card (NIC) device.
 */
public interface NicDevice extends Comparable {

    /**
     * Maximum link speed in Mbps.
     */
    static final long MAX_SPEED = 400000;

    /**
     * Returns the name of this NIC.
     *
     * @return NIC name
     */
    String name();

    /**
     * Returns the port number of this NIC.
     *
     * @return NIC port number
     */
    long portNumber();

    /**
     * Returns the type of the port of this NIC.
     *
     * @return NIC port type
     */
    Type portType();

    /**
     * Returns the speed of the NIC in Mbps.
     *
     * @return NIC speed in Mbps
     */
    long speed();

    /**
     * Returns the current status of the NIC.
     *
     * @return NIC status (up=true, down=false)
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
     * @return hardware address of the NIC
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
