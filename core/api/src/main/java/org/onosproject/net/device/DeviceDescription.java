/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.device;

import org.onosproject.net.Description;
import org.onosproject.net.Device;
import org.onlab.packet.ChassisId;

import java.net.URI;

/**
 * Carrier of immutable information about a device.
 */
public interface DeviceDescription extends Description {

    /**
     * Protocol/provider specific URI that can be used to encode the identity
     * information required to communicate with the device externally, e.g.
     * datapath ID.
     *
     * @return provider specific URI for the device
     */
    URI deviceUri();

    /**
     * Returns the type of the infrastructure device.
     *
     * @return type of the device
     */
    Device.Type type();

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
     * Returns a device chassis id.
     *
     * @return chassis id
     */
    ChassisId chassisId();

    /**
     * Return whether device should be made available by default.
     *
     * @return default availability
     */
    boolean isDefaultAvailable();

}
