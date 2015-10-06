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
package org.onosproject.net.config.basics;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;

/**
 * Basic configuration for network infrastructure devices.
 */
public class BasicDeviceConfig extends BasicElementConfig<DeviceId> {

    public static final String TYPE = "type";
    public static final String DRIVER = "driver";
    public static final String MANAGEMENT_ADDRESS = "managementAddress";

    /**
     * Returns the device type.
     *
     * @return device type override
     */
    public Device.Type type() {
        return get(TYPE, Device.Type.SWITCH, Device.Type.class);
    }

    /**
     * Sets the device type.
     *
     * @param type device type override
     * @return self
     */
    public BasicDeviceConfig type(Device.Type type) {
        return (BasicDeviceConfig) setOrClear(TYPE, type);
    }

    /**
     * Returns the device driver name.
     *
     * @return driver name of null if not set
     */
    public String driver() {
        return get(DRIVER, subject.toString());
    }

    /**
     * Sets the driver name.
     *
     * @param driverName new driver name; null to clear
     * @return self
     */
    public BasicElementConfig driver(String driverName) {
        return (BasicElementConfig) setOrClear(DRIVER, driverName);
    }

    /**
     * Returns the device management ip (ip:port).
     *
     * @return device management address (ip:port) or null if not set
     */
    public String managementAddress() {
        return get(MANAGEMENT_ADDRESS, null);
    }

    /**
     * Sets the driver name.
     *
     * @param managementAddress new device management address (ip:port); null to clear
     * @return self
     */
    public BasicElementConfig managementAddress(String managementAddress) {
        return (BasicElementConfig) setOrClear(MANAGEMENT_ADDRESS, managementAddress);
    }

    // TODO: device port meta-data to be configured via BasicPortsConfig
    // TODO: device credentials/keys

}
