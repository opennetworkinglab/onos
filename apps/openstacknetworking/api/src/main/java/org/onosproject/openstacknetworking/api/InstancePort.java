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
package org.onosproject.openstacknetworking.api;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

/**
 * Representation of virtual instance port.
 */
public interface InstancePort {

    /**
     * List of instance port states.
     */
    enum State {

        /**
         * Signifies that the given instance port is in active state.
         */
        ACTIVE,

        /**
         * Signifies that the given instance port is in inactive state due to
         * host termination.
         */
        INACTIVE,

        /**
         * Signifies that the given instance port is in migrating state.
         */
        MIGRATING,

        /**
         * Signifies that the given instance port will be removed soon.
         */
        REMOVED,

        /**
         * Signifies that the given instance port has been migrated.
         */
        MIGRATED,

        /**
         * Signifies that the given instance port in the pending status for removal.
         */
        REMOVE_PENDING,
    }

    /**
     * Returns the OpenStack network ID of the instance port.
     *
     * @return openstack network id
     */
    String networkId();

    /**
     * Returns the OpenStack port ID of a given host.
     *
     * @return openstack port id
     */
    String portId();

    /**
     * Returns the MAC address of the instance port.
     *
     * @return mac address
     */
    MacAddress macAddress();

    /**
     * Returns the IP address of the instance port.
     *
     * @return ip address
     */
    IpAddress ipAddress();

    /**
     * Returns the device ID of the instance port.
     *
     * @return device id
     */
    DeviceId deviceId();

    /**
     * Returns the old device ID of the instance port.
     * This method returns valid value only if the VM is in migration phase.
     *
     * @return device id
     */
    DeviceId oldDeviceId();

    /**
     * Returns the port number of the instance port.
     *
     * @return port number
     */
    PortNumber portNumber();

    /**
     * Returns the old port number of the instance port.
     * This method returns valid value only if the VM is in migration phase.
     *
     * @return port number
     */
    PortNumber oldPortNumber();

    /**
     * Returns the state of the instance port.
     *
     * @return state of port
     */
    State state();

    /**
     * Returns new instance port instance with given state.
     *
     * @param newState updated state
     * @return updated instance port
     */
    InstancePort updateState(State newState);

    /**
     * Returns new instance port instance with the given prev location data.
     *
     * @param oldDeviceId       old device ID
     * @param oldPortNumber     old port number
     * @return updated instance port
     */
    InstancePort updatePrevLocation(DeviceId oldDeviceId, PortNumber oldPortNumber);

    /**
     * Builder of new instance port.
     */
    interface Builder {

        /**
         * Builds an immutable instance port instance.
         *
         * @return instance port
         */
        InstancePort build();

        /**
         * Returns instance port builder with supplied network identifier.
         *
         * @param networkId network identifier
         * @return instance port builder
         */
        Builder networkId(String networkId);

        /**
         * Returns instance port builder with supplied port identifier.
         *
         * @param portId port identifier
         * @return instance port builder
         */
        Builder portId(String portId);

        /**
         * Returns instance port builder with supplied Mac Address.
         *
         * @param macAddress MAC address
         * @return instance port builder
         */
        Builder macAddress(MacAddress macAddress);

        /**
         * Returns instance port builder with supplied IP Address.
         *
         * @param ipAddress IP address
         * @return instance port builder
         */
        Builder ipAddress(IpAddress ipAddress);

        /**
         * Returns instance port builder with supplied Device identifier.
         *
         * @param deviceId device identifier
         * @return instance port builder
         */
        Builder deviceId(DeviceId deviceId);

        /**
         * Returns instance port builder with supplied old Device identifier.
         *
         * @param oldDeviceId device identifier
         * @return instance port builder
         */
        Builder oldDeviceId(DeviceId oldDeviceId);

        /**
         * Returns instance port builder with supplied port number.
         *
         * @param portNumber port number
         * @return instance port builder
         */
        Builder portNumber(PortNumber portNumber);

        /**
         * Returns instance port builder with supplied old port number.
         *
         * @param oldPortNumber port number
         * @return instance port builder
         */
        Builder oldPortNumber(PortNumber oldPortNumber);

        /**
         * Returns instance port builder with supplied state.
         *
         * @param state state
         * @return instance port builder
         */
        Builder state(State state);
    }
}
