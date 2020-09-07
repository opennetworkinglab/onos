/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.api;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

/**
 * Representation of virtual port.
 */
public interface K8sPort {

    /**
     * List of instance port state.
     */
    enum State {

        /**
         * Signifies that the given port is in active state.
         */
        ACTIVE,

        /**
         * Signifies that the given port is in inactive state.
         */
        INACTIVE,
    }

    /**
     * Returns the kubernetes network ID that the port is associated with.
     *
     * @return kubernetes network ID
     */
    String networkId();

    /**
     * Returns the kubernetes port ID.
     *
     * @return kubernetes port ID
     */
    String portId();

    /**
     * Returns the MAC address of the port.
     *
     * @return MAC address
     */
    MacAddress macAddress();

    /**
     * Returns the IP address of the port.
     *
     * @return IP address
     */
    IpAddress ipAddress();

    /**
     * Returns the device ID of the port.
     *
     * @return device ID
     */
    DeviceId deviceId();

    /**
     * Returns the port number of the port.
     *
     * @return port number
     */
    PortNumber portNumber();

    /**
     * Returns the state of the port.
     *
     * @return state of port
     */
    State state();

    /**
     * Returns new port instance with the given state.
     *
     * @param newState updated state
     * @return updated port
     */
    K8sPort updateState(State newState);

    /**
     * Returns new port instance with the given port number.
     *
     * @param portNumber updated port number
     * @return updated port
     */
    K8sPort updatePortNumber(PortNumber portNumber);

    /**
     * Returns new port instance with the given device ID.
     *
     * @param deviceId device identifier
     * @return updated port
     */
    K8sPort updateDeviceId(DeviceId deviceId);

    /**
     * Builder of new port.
     */
    interface Builder {

        /**
         * Builds an immutable port instance.
         *
         * @return kubernetes port
         */
        K8sPort build();

        /**
         * Returns port builder with supplied network ID.
         *
         * @param networkId network ID
         * @return port builder
         */
        Builder networkId(String networkId);

        /**
         * Returns port builder with supplied port ID.
         *
         * @param portId port ID
         * @return port builder
         */
        Builder portId(String portId);

        /**
         * Returns port builder with supplied MAC address.
         *
         * @param macAddress MAC address
         * @return port builder
         */
        Builder macAddress(MacAddress macAddress);

        /**
         * Returns port builder with supplied IP address.
         *
         * @param ipAddress IP address
         * @return port builder
         */
        Builder ipAddress(IpAddress ipAddress);

        /**
         * Returns port builder with supplied device ID.
         *
         * @param deviceId device ID
         * @return port builder
         */
        Builder deviceId(DeviceId deviceId);

        /**
         * Returns port builder with supplied port number.
         *
         * @param portNumber port number
         * @return port builder
         */
        Builder portNumber(PortNumber portNumber);

        /**
         * Returns port builder with supplied port state.
         *
         * @param state port state
         * @return port builder
         */
        Builder state(State state);
    }
}
