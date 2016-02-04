/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.incubator.net.key;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for DeviceKey.
 */
public class DeviceKeyTest {

    final String deviceKeyIdValue = "DeviceKeyId1";
    final String deviceKeyLabel = "DeviceKeyLabel";
    final String deviceKeySnmpName = "DeviceKeySnmpName";

    /**
     * Checks the construction of a device key name with a null
     * value passed into it. This will throw a NullPointerException.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateDeviceKeyUsingCommunityNameWithNull() {
        DeviceKey deviceKey = DeviceKey.createDeviceKeyUsingCommunityName(null, null, null);
    }

    /**
     * Checks the construction of a device key name with non-null
     * values passed into it.
     */
    @Test
    public void testCreateDeviceKeyUsingCommunityName() {
        DeviceKeyId deviceKeyId = DeviceKeyId.deviceKeyId(deviceKeyIdValue);

        DeviceKey deviceKey = DeviceKey.createDeviceKeyUsingCommunityName(deviceKeyId,
                                                                          deviceKeyLabel, deviceKeySnmpName);
        assertNotNull("The deviceKey should not be null.", deviceKey);
        assertEquals("The deviceKeyId should match as expected", deviceKeyId, deviceKey.deviceKeyId());
        assertEquals("The label should match as expected", deviceKeyLabel, deviceKey.label());
        assertEquals("The type should match as expected", DeviceKey.Type.COMMUNITY_NAME, deviceKey.type());

        CommunityName communityName = deviceKey.asCommunityName();
        assertNotNull("The communityName should not be null.", communityName);
        assertEquals("The name should match as expected", deviceKeySnmpName, communityName.name());
    }
}
