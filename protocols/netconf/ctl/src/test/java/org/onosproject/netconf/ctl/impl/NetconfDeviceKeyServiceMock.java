/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.netconf.ctl.impl;

import org.onlab.packet.IpAddress;
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.net.key.DeviceKeyListener;
import org.onosproject.net.key.DeviceKeyService;
import org.onosproject.netconf.NetconfDeviceInfo;

import java.util.Collection;

/**
 * Mock DeviceKey service to return device keys.
 */
class NetconfDeviceKeyServiceMock implements DeviceKeyService {

    private static final String DEVICE_1_IP = "10.10.10.11";
    private static final String DEVICE_2_IP = "10.10.10.12";
    private static final String BAD_DEVICE_IP = "10.10.10.13";
    private static final String DEVICE_IPV6 = "2001:db8::1";

    private static final int DEVICE_1_PORT = 11;
    private static final int DEVICE_2_PORT = 12;
    private static final int BAD_DEVICE_PORT = 13;
    private static final int IPV6_DEVICE_PORT = 14;

    //DeviceInfo
    private NetconfDeviceInfo deviceInfo1 =
            new NetconfDeviceInfo("device1", "001", IpAddress.valueOf(DEVICE_1_IP),
                                  DEVICE_1_PORT);
    private NetconfDeviceInfo deviceInfo2 =
            new NetconfDeviceInfo("device2", "002", IpAddress.valueOf(DEVICE_2_IP),
                                  DEVICE_2_PORT);
    private NetconfDeviceInfo badDeviceInfo3 =
            new NetconfDeviceInfo("device3", "003", IpAddress.valueOf(BAD_DEVICE_IP),
                                  BAD_DEVICE_PORT);
    private NetconfDeviceInfo deviceInfoIpV6 =
            new NetconfDeviceInfo("deviceIpv6", "004", IpAddress.valueOf(DEVICE_IPV6), IPV6_DEVICE_PORT);

    @Override
    public Collection<DeviceKey> getDeviceKeys() {
        return null;
    }

    @Override
    public DeviceKey getDeviceKey(DeviceKeyId deviceKeyId) {
        if (deviceKeyId.toString().equals(deviceInfo1.getDeviceId().toString())) {
            return DeviceKey.createDeviceKeyUsingUsernamePassword(
                    DeviceKeyId.deviceKeyId(deviceInfo1.getDeviceId().toString()),
                    null, deviceInfo1.name(), deviceInfo1.password());
        } else if (deviceKeyId.toString().equals(deviceInfo2.getDeviceId().toString())) {
            return DeviceKey.createDeviceKeyUsingUsernamePassword(
                    DeviceKeyId.deviceKeyId(deviceInfo2.getDeviceId().toString()),
                    null, deviceInfo2.name(), deviceInfo2.password());
        } else if (deviceKeyId.toString().equals(badDeviceInfo3.getDeviceId().toString())) {
            return DeviceKey.createDeviceKeyUsingUsernamePassword(
                    DeviceKeyId.deviceKeyId(badDeviceInfo3.getDeviceId().toString()),
                    null, badDeviceInfo3.name(), badDeviceInfo3.password());
        } else if (deviceKeyId.toString().equals(deviceInfoIpV6.getDeviceId().toString())) {
            return DeviceKey.createDeviceKeyUsingUsernamePassword(
                    DeviceKeyId.deviceKeyId(deviceInfoIpV6.getDeviceId().toString()),
                    null, deviceInfoIpV6.name(), deviceInfoIpV6.password());
        }
        return null;
    }

    @Override
    public void addListener(DeviceKeyListener listener) {

    }

    @Override
    public void removeListener(DeviceKeyListener listener) {

    }
}
