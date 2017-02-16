/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.onosproject.net.key.DeviceKeyId;

/**
 * Basic configuration for network infrastructure devices.
 */
public final class BasicDeviceConfig extends BasicElementConfig<DeviceId> {

    private static final String TYPE = "type";
    private static final String DRIVER = "driver";
    private static final String MANAGEMENT_ADDRESS = "managementAddress";
    private static final String MANUFACTURER = "manufacturer";
    private static final String HW_VERSION = "hwVersion";
    private static final String SW_VERSION = "swVersion";
    private static final String SERIAL = "serial";
    private static final String DEVICE_KEY_ID = "deviceKeyId";

    @Override
    public boolean isValid() {
        return hasOnlyFields(ALLOWED, NAME, LOC_TYPE, LATITUDE, LONGITUDE,
                GRID_Y, GRID_X, UI_TYPE, RACK_ADDRESS, OWNER, TYPE, DRIVER,
                MANUFACTURER, HW_VERSION, SW_VERSION, SERIAL,
                MANAGEMENT_ADDRESS, DEVICE_KEY_ID);
    }

    /**
     * Returns the device type.
     *
     * @return device type override
     */
    public Device.Type type() {
        return get(TYPE, null, Device.Type.class);
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
     * @return driver name or null if not set
     */
    public String driver() {
        return get(DRIVER, null);
    }

    /**
     * Sets the driver name.
     *
     * @param driverName new driver name; null to clear
     * @return self
     */
    public BasicDeviceConfig driver(String driverName) {
        return (BasicDeviceConfig) setOrClear(DRIVER, driverName);
    }

    /**
     * Returns the device manufacturer.
     *
     * @return manufacturer or null if not set
     */
    public String manufacturer() {
        return get(MANUFACTURER, null);
    }

    /**
     * Sets the device manufacturer.
     *
     * @param manufacturerName new manufacturer; null to clear
     * @return self
     */
    public BasicDeviceConfig manufacturer(String manufacturerName) {
        return (BasicDeviceConfig) setOrClear(MANUFACTURER, manufacturerName);
    }

    /**
     * Returns the device hardware version.
     *
     * @return hardware version or null if not set
     */
    public String hwVersion() {
        return get(HW_VERSION, null);
    }

    /**
     * Sets the device hardware version.
     *
     * @param hwVersion new hardware version; null to clear
     * @return self
     */
    public BasicDeviceConfig hwVersion(String hwVersion) {
        return (BasicDeviceConfig) setOrClear(HW_VERSION, hwVersion);
    }

    /**
     * Returns the device software version.
     *
     * @return software version or null if not set
     */
    public String swVersion() {
        return get(SW_VERSION, null);
    }

    /**
     * Sets the device software version.
     *
     * @param swVersion new software version; null to clear
     * @return self
     */
    public BasicDeviceConfig swVersion(String swVersion) {
        return (BasicDeviceConfig) setOrClear(SW_VERSION, swVersion);
    }

    /**
     * Returns the device serial number.
     *
     * @return serial number or null if not set
     */
    public String serial() {
        return get(SERIAL, null);
    }

    /**
     * Sets the device serial number.
     *
     * @param serial new serial number; null to clear
     * @return self
     */
    public BasicDeviceConfig serial(String serial) {
        return (BasicDeviceConfig) setOrClear(SERIAL, serial);
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
     * Sets the device management ip (ip:port).
     *
     * @param managementAddress new device management address (ip:port); null to clear
     * @return self
     */
    public BasicDeviceConfig managementAddress(String managementAddress) {
        return (BasicDeviceConfig) setOrClear(MANAGEMENT_ADDRESS, managementAddress);
    }

    /**
     * Returns the device key id.
     *
     * @return device key id or null if not set
     */
    public DeviceKeyId deviceKeyId() {
        String s = get(DEVICE_KEY_ID, null);
        return s == null ? null : DeviceKeyId.deviceKeyId(s);
    }

    /**
     * Sets the device key id.
     *
     * @param deviceKeyId the new device key id; null to clear
     * @return self
     */
    public BasicDeviceConfig deviceKeyId(DeviceKeyId deviceKeyId) {
        return (BasicDeviceConfig) setOrClear(DEVICE_KEY_ID,
                deviceKeyId != null ? deviceKeyId.id() : null);
    }

    // TODO: device port meta-data to be configured via BasicPortsConfig
    // TODO: device credentials/keys; in a separate config

}
