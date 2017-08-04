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

package org.onosproject.net.key;

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
    final String deviceKeyUsername = "DeviceKeyUsername";
    final String deviceKeyPassword = "DeviceKeyPassword";

    /**
     * Checks the construction of a community name device key with a null
     * value passed into it. This will throw a NullPointerException.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateDeviceKeyUsingCommunityNameWithNull() {
        DeviceKey deviceKey = DeviceKey.createDeviceKeyUsingCommunityName(null, null, null);
    }

    /**
     * Checks the construction of a community name device key name with non-null
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

    /**
     * Checks the invalid conversion a device key of type COMMUNITY_NAME to
     * a username / password object.
     */
    @Test(expected = IllegalStateException.class)
    public void testInvalidConversionToAsUsernamePassword() {
        DeviceKeyId deviceKeyId = DeviceKeyId.deviceKeyId(deviceKeyIdValue);

        DeviceKey deviceKey = DeviceKey.createDeviceKeyUsingCommunityName(deviceKeyId,
                                                                          deviceKeyLabel, deviceKeySnmpName);
        // Attempting to convert this device key to a username / password object
        // should throw and IllegalStateException.
        UsernamePassword usernamePassword = deviceKey.asUsernamePassword();
    }

    /**
     * Checks the construction of a username / password device key with a null
     * value passed into it. This will throw a NullPointerException.
     */
    @Test(expected = NullPointerException.class)
    public void testCreateDeviceKeyUsingUsernamePasswordWithNull() {
        DeviceKey deviceKey = DeviceKey.createDeviceKeyUsingUsernamePassword(null, null, null, null);
    }

    /**
     * Checks the construction of a username and password device key name with non-null
     * values passed into it.
     */
    @Test
    public void testCreateDeviceKeyUsingUsernamePassword() {
        DeviceKeyId deviceKeyId = DeviceKeyId.deviceKeyId(deviceKeyIdValue);

        DeviceKey deviceKey = DeviceKey.createDeviceKeyUsingUsernamePassword(deviceKeyId, deviceKeyLabel,
                                                                             deviceKeyUsername, deviceKeyPassword);
        assertNotNull("The deviceKey should not be null.", deviceKey);
        assertEquals("The deviceKeyId should match as expected", deviceKeyId, deviceKey.deviceKeyId());
        assertEquals("The label should match as expected", deviceKeyLabel, deviceKey.label());
        assertEquals("The type should match as expected", DeviceKey.Type.USERNAME_PASSWORD, deviceKey.type());

        UsernamePassword usernamePassword = deviceKey.asUsernamePassword();
        assertNotNull("The usernamePassword should not be null.", usernamePassword);
        assertEquals("The username should match as expected", deviceKeyUsername, usernamePassword.username());
        assertEquals("The password should match as expected", deviceKeyPassword, usernamePassword.password());
    }

    /**
     * Checks the invalid conversion a device key of type USERNAME_PASSWORD to
     * a community name object.
     */
    @Test(expected = IllegalStateException.class)
    public void testInvalidConversionToAsCommunityName() {
        DeviceKeyId deviceKeyId = DeviceKeyId.deviceKeyId(deviceKeyIdValue);

        DeviceKey deviceKey = DeviceKey.createDeviceKeyUsingUsernamePassword(deviceKeyId, deviceKeyLabel,
                                                                             deviceKeyUsername, deviceKeyPassword);
        // Attempting to convert this device key to a community name should throw and IllegalStateException.
        CommunityName communityName = deviceKey.asCommunityName();
    }
}
