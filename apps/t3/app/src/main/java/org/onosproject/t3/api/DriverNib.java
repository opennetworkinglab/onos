/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.t3.api;

import com.google.common.collect.ImmutableMap;
import org.onosproject.net.DeviceId;

import java.util.Map;

/**
 * Represents Network Information Base (NIB) for drivers
 * and supports alternative functions to
 * {@link org.onosproject.net.driver.DriverService} for offline data.
 */
public class DriverNib extends AbstractNib {

    private Map<DeviceId, String> deviceDriverMap;

    // use the singleton helper to create the instance
    protected DriverNib() {
    }

    /**
     * Sets a map of device id : driver name.
     *
     * @param deviceDriverMap device-driver map
     */
    public void setDeviceDriverMap(Map<DeviceId, String> deviceDriverMap) {
        this.deviceDriverMap = deviceDriverMap;
    }

    /**
     * Returns the device-driver map.
     *
     * @return device-driver map
     */
    public Map<DeviceId, String> getDeviceDriverMap() {
        return ImmutableMap.copyOf(deviceDriverMap);
    }

    /**
     * Returns a driver name of the given device.
     *
     * @param deviceId the device id
     * @return the driver name
     */
    public String getDriverName(DeviceId deviceId) {
        return deviceDriverMap.get(deviceId);
    }

    /**
     * Returns the singleton instance of drivers NIB.
     *
     * @return instance of drivers NIB
     */
    public static DriverNib getInstance() {
        return DriverNib.SingletonHelper.INSTANCE;
    }

    private static class SingletonHelper {
        private static final DriverNib INSTANCE = new DriverNib();
    }

}
