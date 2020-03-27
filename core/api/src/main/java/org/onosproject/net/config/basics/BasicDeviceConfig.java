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
package org.onosproject.net.config.basics;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.key.DeviceKeyId;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Basic configuration for network infrastructure devices.
 */
public final class BasicDeviceConfig extends BasicElementConfig<DeviceId> {

    private static final String TYPE = "type";
    private static final String DRIVER = "driver";
    private static final String MANAGEMENT_ADDRESS = "managementAddress";
    private static final String PIPECONF = "pipeconf";
    private static final String MANUFACTURER = "manufacturer";
    private static final String HW_VERSION = "hwVersion";
    private static final String SW_VERSION = "swVersion";
    private static final String SERIAL = "serial";
    private static final String PURGE_ON_DISCONNECT = "purgeOnDisconnection";
    private static final String DEVICE_KEY_ID = "deviceKeyId";

    private static final int DRIVER_MAX_LENGTH = 256;
    private static final int MANUFACTURER_MAX_LENGTH = 256;
    private static final int HW_VERSION_MAX_LENGTH = 256;
    private static final int SW_VERSION_MAX_LENGTH = 256;
    private static final int SERIAL_MAX_LENGTH = 256;
    private static final int MANAGEMENT_ADDRESS_MAX_LENGTH = 1024;
    private static final int PIPECONF_MAX_LENGTH = 256;

    @Override
    public boolean isValid() {
        // Validate type/DeviceKeyId
        type();
        deviceKeyId();

        return super.isValid()
                && hasOnlyFields(ALLOWED, NAME, LOC_TYPE, LATITUDE, LONGITUDE,
                GRID_Y, GRID_X, UI_TYPE, RACK_ADDRESS, OWNER, TYPE, DRIVER, ROLES,
                MANUFACTURER, HW_VERSION, SW_VERSION, SERIAL,
                MANAGEMENT_ADDRESS, PIPECONF, DEVICE_KEY_ID, PURGE_ON_DISCONNECT)
                && isValidLength(DRIVER, DRIVER_MAX_LENGTH)
                && isValidLength(MANUFACTURER, MANUFACTURER_MAX_LENGTH)
                && isValidLength(HW_VERSION, MANUFACTURER_MAX_LENGTH)
                && isValidLength(SW_VERSION, MANUFACTURER_MAX_LENGTH)
                && isValidLength(SERIAL, MANUFACTURER_MAX_LENGTH)
                && isValidLength(MANAGEMENT_ADDRESS, MANAGEMENT_ADDRESS_MAX_LENGTH)
                && isValidLength(PIPECONF, PIPECONF_MAX_LENGTH);
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
        checkArgument(driverName.length() <= DRIVER_MAX_LENGTH,
                "driver exceeds maximum length " + DRIVER_MAX_LENGTH);
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
        checkArgument(manufacturerName.length() <= MANUFACTURER_MAX_LENGTH,
                "manufacturer exceeds maximum length " + MANUFACTURER_MAX_LENGTH);
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
        checkArgument(hwVersion.length() <= HW_VERSION_MAX_LENGTH,
                "hwVersion exceeds maximum length " + HW_VERSION_MAX_LENGTH);
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
        checkArgument(swVersion.length() <= SW_VERSION_MAX_LENGTH,
                "swVersion exceeds maximum length " + SW_VERSION_MAX_LENGTH);
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
        checkArgument(serial.length() <= SERIAL_MAX_LENGTH,
                "serial exceeds maximum length " + SERIAL_MAX_LENGTH);
        return (BasicDeviceConfig) setOrClear(SERIAL, serial);
    }

    /**
     * Returns the device management address (e.g, "ip:port" or full URI
     * string).
     *
     * @return device management address or null if not set
     */
    public String managementAddress() {
        return get(MANAGEMENT_ADDRESS, null);
    }

    /**
     * Returns the device pipeconf.
     *
     * @return device pipeconf or null if not set
     */
    public String pipeconf() {
        return get(PIPECONF, null);
    }

    /**
     * Sets the device management ip (ip:port).
     *
     * @param managementAddress new device management address (ip:port); null to clear
     * @return self
     */
    public BasicDeviceConfig managementAddress(String managementAddress) {
        checkArgument(managementAddress.length() <= MANAGEMENT_ADDRESS_MAX_LENGTH,
                "managementAddress exceeds maximum length " + MANAGEMENT_ADDRESS_MAX_LENGTH);
        return (BasicDeviceConfig) setOrClear(MANAGEMENT_ADDRESS, managementAddress);
    }

    /**
     * Sets the device pipeconf.
     *
     * @param pipeconf new device pipeconf
     * @return self
     */
    public BasicDeviceConfig pipeconf(String pipeconf) {
        checkArgument(pipeconf.length() <= PIPECONF_MAX_LENGTH,
                      "pipeconf exceeds maximum length " + MANAGEMENT_ADDRESS_MAX_LENGTH);
        return (BasicDeviceConfig) setOrClear(PIPECONF, pipeconf);
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

    /**
     * Returns the device purgeOnDisconnection flag for this device.
     *
     * @return device purgeOnDisconnection, false if not set.
     */
    public boolean purgeOnDisconnection() {
        return get(PURGE_ON_DISCONNECT, false);
    }

    /**
     * Sets the purgeOnDisconnection flag for the device.
     *
     * @param purgeOnDisconnection purges flows, groups, meters on disconnection.
     * @return self
     */
    public BasicDeviceConfig purgeOnDisconnection(boolean purgeOnDisconnection) {
        return (BasicDeviceConfig) setOrClear(PURGE_ON_DISCONNECT, purgeOnDisconnection);
    }

    /**
     * Returns if the device purgeOnDisconnection flag for this device has been explicitly configured.
     *
     * @return device purgeOnDisconnection explicitly configured, false if not.
     */
    public boolean isPurgeOnDisconnectionConfigured() {
        return hasField(PURGE_ON_DISCONNECT);
    }

    // TODO: device port meta-data to be configured via BasicPortsConfig
    // TODO: device credentials/keys; in a separate config

}
