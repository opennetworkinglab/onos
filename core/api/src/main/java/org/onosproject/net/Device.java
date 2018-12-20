/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net;

import org.onlab.packet.ChassisId;

/**
 * Representation of a network infrastructure device.
 */
public interface Device extends Element {

    /**
     * Coarse classification of the type of the infrastructure device.
     */
    enum Type {
        SWITCH, ROUTER, ROADM, OTN, ROADM_OTN, FIREWALL, BALANCER, IPS, IDS, CONTROLLER,
        VIRTUAL, FIBER_SWITCH, MICROWAVE, OLT, ONU, OPTICAL_AMPLIFIER, OLS, TERMINAL_DEVICE,
        OTHER, SERVER
    }

    /**
     * Returns the device identifier.
     *
     * @return device id
     */
    @Override
    DeviceId id();

    /**
     * Returns the type of the infrastructure device.
     *
     * @return type of the device
     */
    Type type();

    /**
     * Returns the device manufacturer name.
     *
     * @return manufacturer name
     */
    String manufacturer();

    /**
     * Returns the device hardware version.
     *
     * @return hardware version
     */
    String hwVersion();

    /**
     * Returns the device software version.
     *
     * @return software version
     */
    String swVersion();

    /**
     * Returns the device serial number.
     *
     * @return serial number
     */
    String serialNumber();

    /**
     * Returns the device chassis id.
     *
     * @return chassis id
     */
    ChassisId chassisId();

}
