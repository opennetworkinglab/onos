/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.DeviceId;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Test of the default virtual device model entity.
 */
public class DefaultVirtualDeviceTest {
    final String deviceIdValue1 = "DEVICE_ID1";
    final String deviceIdValue2 = "DEVICE_ID2";

    /**
     * Checks that the DefaultVirtualDevice class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultVirtualDevice.class);
    }

    @Test
    public void testEquality() {
        DefaultVirtualDevice device1 =
                new DefaultVirtualDevice(NetworkId.networkId(0), DeviceId.deviceId(deviceIdValue1));
        DefaultVirtualDevice device2 =
                new DefaultVirtualDevice(NetworkId.networkId(0), DeviceId.deviceId(deviceIdValue1));
        DefaultVirtualDevice device3 =
                new DefaultVirtualDevice(NetworkId.networkId(0), DeviceId.deviceId(deviceIdValue2));
        DefaultVirtualDevice device4 =
                new DefaultVirtualDevice(NetworkId.networkId(1), DeviceId.deviceId(deviceIdValue1));

        new EqualsTester().addEqualityGroup(device1, device2).addEqualityGroup(device3)
                .addEqualityGroup(device4).testEquals();
    }
}
