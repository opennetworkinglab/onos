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
package org.onosproject.store.device.impl;

import static org.onosproject.net.DeviceId.deviceId;

import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;

import com.google.common.testing.EqualsTester;

public class DeviceFragmentIdTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final ProviderId PIDA = new ProviderId("of", "bar", true);
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");

    @Test
    public final void testEquals() {

        new EqualsTester()
            .addEqualityGroup(new DeviceFragmentId(DID1, PID),
                              new DeviceFragmentId(DID1, PID))
            .addEqualityGroup(new DeviceFragmentId(DID2, PID),
                              new DeviceFragmentId(DID2, PID))
            .addEqualityGroup(new DeviceFragmentId(DID1, PIDA),
                              new DeviceFragmentId(DID1, PIDA))
            .addEqualityGroup(new DeviceFragmentId(DID2, PIDA),
                              new DeviceFragmentId(DID2, PIDA))
        .testEquals();
    }

}
