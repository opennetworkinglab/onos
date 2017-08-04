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

import com.google.common.testing.EqualsTester;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.DeviceId.deviceId;

/**
 * Test of the device identifier.
 */
public class DeviceIdTest {

    @Test
    public void basics() {
        new EqualsTester()
                .addEqualityGroup(deviceId("of:foo"),
                                  deviceId("of:foo"))
                .addEqualityGroup(deviceId("of:bar"))
                .testEquals();
    }


    @Test
    public void ipAndPort() {
        DeviceId ipp = deviceId("netconf:127.0.0.1:830");
        assertEquals("127.0.0.1:830", ipp.uri().getSchemeSpecificPart());

        DeviceId ipp6 = deviceId("netconf:[2001:db8:85a3:8d3:1319:8a2e:370:7348]:830");
        Assert.assertEquals("[2001:db8:85a3:8d3:1319:8a2e:370:7348]:830", ipp6.uri().getSchemeSpecificPart());
    }

}
